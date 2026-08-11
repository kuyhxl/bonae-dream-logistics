#!/usr/bin/env bash
#
# E2E 검증용 시드 데이터 생성
#
# 게이트웨이를 경유해 실제 REST API를 호출하므로, 데이터가 만들어지는 과정에서
# 인증·인가·헤더 전파·서비스 간 Feign 호출까지 함께 함검증된다.
#
# 전국 17개 허브와 이동경로는 hub-service의 마이그레이션이 넣는 기준 데이터이므로 여기서는 참조만
#
# 승인 처리와 역할·소속 부여는 해당 API가 아직 없어 그 부분만 DB를 직접 갱신한다!
#
# 사용법:
#   ./scripts/seed-api.sh                 # 기본 (게이트웨이 localhost:8080)
#   GATEWAY=http://localhost:8080 ./scripts/seed-api.sh
#

set -uo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
PG_CONTAINER="${PG_CONTAINER:-bonae-postgres}"
PG_USER="${PG_USER:-bonae}"
PG_DB="${PG_DB:-bonae}"

# 시드 계정 비밀번호. username 규칙은 소문자+숫자 4~10자,
# 비밀번호는 대소문자·숫자·특수문자를 포함한 8~15자여야 한다.
SEED_PASSWORD="Seed1234!"

PASS=0
FAIL=0

# 로그는 stderr로 보낸다. 일부 함수가 stdout으로 생성된 id를 반환하므로
info()  { printf '\033[36m%s\033[0m\n' "$*" >&2; }
ok()    { printf '  \033[32m✓\033[0m %s\n' "$*" >&2; PASS=$((PASS + 1)); }
warn()  { printf '  \033[33m!\033[0m %s\n' "$*" >&2; }
fail()  { printf '  \033[31m✗\033[0m %s\n' "$*" >&2; FAIL=$((FAIL + 1)); }

# psql 실행. 승인 처리처럼 아직 API가 없는 작업에만 사용한다.
psql_exec() {
    docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -t -A -c "$1" 2>/dev/null
}

require_services() {
    info "[0/6] 사전 확인"

    if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${PG_CONTAINER}$"; then
        fail "$PG_CONTAINER 컨테이너가 없습니다. docker compose up -d 를 먼저 실행하세요."
        exit 1
    fi
    ok "postgres 컨테이너 확인"

    # curl은 접속 실패 시에도 %{http_code}로 000을 출력하므로 별도 기본값을 붙이지 않는다.
    local code
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$GATEWAY/actuator/health")
    if [ "$code" != "200" ]; then
        fail "게이트웨이에 접속할 수 없습니다 ($GATEWAY, HTTP $code)"
        exit 1
    fi
    ok "게이트웨이 응답 확인"
}

# 회원가입 -> 승인 -> 역할 부여까지 처리한다.
# 승인/역할 부여 API가 아직 없어 이 부분만 DB를 직접 갱신한다.
signup_and_approve() {
    local username=$1 name=$2 role=$3 hub_id=${4:-} company_id=${5:-}

    local body code
    body=$(curl -s -w '\n%{http_code}' -X POST "$GATEWAY/api/auth/signup" \
        -H 'Content-Type: application/json' \
        -d "{\"username\":\"$username\",\"password\":\"$SEED_PASSWORD\",\"name\":\"$name\",\"slackId\":\"U0SEED$(echo "$username" | tr 'a-z' 'A-Z')\",\"affiliationName\":\"시드조직\"}")
    code=$(printf '%s' "$body" | tail -n1)

    if [ "$code" = "201" ]; then
        ok "회원가입: $username ($role)"
    elif [ "$code" = "409" ]; then
        warn "이미 존재: $username — 기존 계정을 재사용합니다"
    else
        fail "회원가입 실패: $username (HTTP $code)"
        return 1
    fi

    # 승인 + 역할/소속 부여
    local set_clause="status='APPROVED', role='$role', approved_by='seedscript', approved_at=now()"
    if [ -n "$hub_id" ]; then
        set_clause="$set_clause, hub_id='$hub_id'"
    fi
    if [ -n "$company_id" ]; then
        set_clause="$set_clause, company_id='$company_id'"
    fi

    psql_exec "UPDATE user_service.p_users SET $set_clause WHERE username='$username';" >/dev/null
    ok "승인 및 역할 부여: $username -> $role"
}

login() {
    local username=$1
    curl -s -X POST "$GATEWAY/api/auth/login" \
        -H 'Content-Type: application/json' \
        -d "{\"username\":\"$username\",\"password\":\"$SEED_PASSWORD\"}" \
        | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

# 전국 17개 허브는 hub-service의 V3 마이그레이션이 이미 넣어둔 기준 데이터다.
# 여기서 새로 만들지 않고 이름으로 찾아 쓴다.
find_hub() {
    local name=$1

    local id
    id=$(psql_exec "SELECT id FROM hub_service.p_hubs WHERE name='$name' AND deleted_at IS NULL LIMIT 1;")
    if [ -z "$id" ]; then
        fail "허브를 찾을 수 없습니다: $name (마이그레이션 V3 적용 여부를 확인하세요)"
        return 1
    fi

    ok "허브 참조: $name"
    printf '%s' "$id"
}

# company-service가 hub-service를 Feign으로 호출해 허브 존재를 검증하므로,
# 이 단계가 성공하면 서비스 간 연동도 함께 확인된거
ensure_company() {
    local token=$1 name=$2 type=$3 hub_id=$4 address=$5

    local existing
    existing=$(psql_exec "SELECT id FROM company_service.p_companies WHERE name='$name' AND deleted_at IS NULL LIMIT 1;")
    if [ -n "$existing" ]; then
        warn "업체 이미 존재: $name"
        printf '%s' "$existing"
        return 0
    fi

    local body code id
    body=$(curl -s -w '\n%{http_code}' -X POST "$GATEWAY/api/companies" \
        -H "Authorization: Bearer $token" \
        -H 'Content-Type: application/json' \
        -d "{\"name\":\"$name\",\"type\":\"$type\",\"hubId\":\"$hub_id\",\"address\":\"$address\"}")
    code=$(printf '%s' "$body" | tail -n1)
    id=$(printf '%s' "$body" | sed -n 's/.*"companyId":"\([^"]*\)".*/\1/p')

    if [ "$code" = "201" ] && [ -n "$id" ]; then
        ok "업체 생성: $name ($type)"
        printf '%s' "$id"
        return 0
    fi

    if [ "$code" = "409" ]; then
        existing=$(psql_exec "SELECT id FROM company_service.p_companies WHERE name='$name' AND deleted_at IS NULL LIMIT 1;")
        if [ -n "$existing" ]; then
            warn "업체 중복 응답(409) — 기존 업체를 재사용: $name"
            printf '%s' "$existing"
            return 0
        fi
    fi

    fail "업체 생성 실패: $name (HTTP $code)"
    return 1
}

# 배송 담당자는 user-service의 사용자 ID를 그대로 PK로 사용함
ensure_delivery_manager() {
    local token=$1 username=$2 manager_type=$3 hub_id=${4:-null}

    local user_id
    user_id=$(psql_exec "SELECT id FROM user_service.p_users WHERE username='$username' LIMIT 1;")
    if [ -z "$user_id" ]; then
        fail "배송 담당자 등록 실패: $username 사용자를 찾을 수 없습니다"
        return 1
    fi

    local existing
    existing=$(psql_exec "SELECT id FROM delivery_service.p_delivery_managers WHERE id='$user_id' AND deleted_at IS NULL LIMIT 1;")
    if [ -n "$existing" ]; then
        warn "배송 담당자 이미 존재: $username"
        return 0
    fi

    # HUB_DELIVERY는 hubId가 없어야 하고 COMPANY_DELIVERY는 있어야 한다.
    local hub_field="null"
    if [ "$hub_id" != "null" ]; then
        hub_field="\"$hub_id\""
    fi

    # delivery_sequence는 기존 최대값 + 1 (유니크 제약 회피)
    local next_seq
    next_seq=$(psql_exec "SELECT COALESCE(MAX(delivery_sequence), -1) + 1 FROM delivery_service.p_delivery_managers;")
    next_seq=${next_seq:-0}

    local body code
    body=$(curl -s -w '\n%{http_code}' -X POST "$GATEWAY/api/delivery-managers" \
        -H "Authorization: Bearer $token" \
        -H 'Content-Type: application/json' \
        -d "{\"deliveryManagerId\":\"$user_id\",\"hubId\":$hub_field,\"managerType\":\"$manager_type\",\"deliverySequence\":$next_seq}")
    code=$(printf '%s' "$body" | tail -n1)

    if [ "$code" = "201" ]; then
        ok "배송 담당자 등록: $username ($manager_type)"
    elif [ "$code" = "404" ] || [ "$code" = "405" ]; then
        warn "배송 담당자 API 미구현 또는 경로 불일치 (HTTP $code) — 건너뜁니다"
    else
        fail "배송 담당자 등록 실패: $username (HTTP $code)"
    fi
}

main() {
    info "보내드림 물류 — E2E 시드 데이터 생성 (API 방식)"
    printf '게이트웨이: %s\n\n' "$GATEWAY"

    require_services
    printf '\n'

    info "[1/6] 사용자 생성"
    # 첫 MASTER는 이후 모든 생성 작업의 주체가 된다
    signup_and_approve "seedmaster" "시드마스터" "MASTER" || exit 1
    printf '\n'

    info "[2/6] 로그인 및 토큰 발급"
    local token
    token=$(login "seedmaster")
    if [ -z "$token" ]; then
        fail "로그인 실패 — 이후 단계를 진행할 수 없습니다"
        exit 1
    fi
    ok "MASTER 토큰 발급 완료"
    printf '\n'

    info "[3/6] 허브 참조 (마이그레이션이 넣은 전국 17개 중)"
    local hub_seoul hub_busan
    hub_seoul=$(find_hub "서울특별시 센터") || exit 1
    hub_busan=$(find_hub "부산광역시 센터") || exit 1
    printf '\n'

    info "[4/6] 업체 생성 (company -> hub Feign 검증 포함)"
    local co_producer co_receiver
    co_producer=$(ensure_company "$token" "시드 생산업체" "PRODUCER" "$hub_seoul" "서울특별시 강남구 시드로 201") || exit 1
    co_receiver=$(ensure_company "$token" "시드 수령업체" "RECEIVER" "$hub_busan" "부산광역시 해운대구 시드로 202") || exit 1
    printf '\n'

    info "[5/6] 역할별 사용자 생성"
    signup_and_approve "seedhub" "시드허브관리자" "HUB_MANAGER" "$hub_seoul"
    signup_and_approve "seedcomp" "시드업체관리자" "COMPANY_MANAGER" "" "$co_producer"
    signup_and_approve "seeddeli" "시드배송담당" "DELIVERY_MANAGER" "$hub_seoul"
    printf '\n'

    info "[6/6] 배송 담당자 등록"
    ensure_delivery_manager "$token" "seeddeli" "HUB_DELIVERY"
    printf '\n'

    info "생성 결과"
    printf '  서울특별시 센터 : %s (기준 데이터)\n' "$hub_seoul"
    printf '  부산광역시 센터 : %s (기준 데이터)\n' "$hub_busan"
    printf '  시드 생산업체   : %s\n' "$co_producer"
    printf '  시드 수령업체   : %s\n' "$co_receiver"
    printf '\n'
    printf '  계정 (비밀번호 공통: %s)\n' "$SEED_PASSWORD"
    printf '    seedmaster / MASTER\n'
    printf '    seedhub    / HUB_MANAGER      (서울특별시 센터 소속)\n'
    printf '    seedcomp   / COMPANY_MANAGER  (시드 생산업체 소속)\n'
    printf '    seeddeli   / DELIVERY_MANAGER (서울특별시 센터 소속)\n'
    printf '\n'
    printf '  토큰 발급 예시:\n'
    printf "    curl -s -X POST %s/api/auth/login -H 'Content-Type: application/json' \\\\\n" "$GATEWAY"
    printf '      -d %s\n' "'{\"username\":\"seedmaster\",\"password\":\"$SEED_PASSWORD\"}'"
    printf '\n'

    if [ "$FAIL" -gt 0 ]; then
        printf '\033[31m실패 %d건, 성공 %d건\033[0m\n' "$FAIL" "$PASS"
        exit 1
    fi
    printf '\033[32m완료 — 성공 %d건\033[0m\n' "$PASS"
}

main "$@"

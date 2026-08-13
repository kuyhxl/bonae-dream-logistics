#!/usr/bin/env bash
#
# E2E 검증용 시드 데이터 생성
#
# 게이트웨이를 경유해 실제 REST API를 호출하므로, 데이터가 만들어지는 과정에서
# 인증·인가·헤더 전파·서비스 간 Feign 호출까지 함께 함검증된다.
#
# 전국 17개 허브와 이동경로는 hub-service의 마이그레이션이 넣는 기준 데이터이므로 여기서는 참조만
#
# 승인·역할·소속 부여도 승인 API(PATCH /api/users/{userId}/approval)로 처리한다
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

# 승인 API는 MASTER를 부여할 수 없어, 마이그레이션이 넣어둔 기준 MASTER 계정으로 승인한다.
MASTER_USERNAME="${MASTER_USERNAME:-master01}"
MASTER_PASSWORD="${MASTER_PASSWORD:-Master1234!}"

PASS=0
FAIL=0

# 로그는 stderr로 보낸다. 일부 함수가 stdout으로 생성된 id를 반환하므로
info()  { printf '\033[36m%s\033[0m\n' "$*" >&2; }
ok()    { printf '  \033[32m✓\033[0m %s\n' "$*" >&2; PASS=$((PASS + 1)); }
warn()  { printf '  \033[33m!\033[0m %s\n' "$*" >&2; }
fail()  { printf '  \033[31m✗\033[0m %s\n' "$*" >&2; FAIL=$((FAIL + 1)); }

# psql 실행. 조회 전용이다. 응답 본문에 없는 id(사용자, 허브)를 찾는 데만 쓰고
# 데이터 변경은 모두 API를 거친다.
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

# 회원가입만 처리한다. 승인은 승인자 토큰이 필요해 별도 함수로 분리했다.
signup() {
    local username=$1 name=$2

    local body code
    body=$(curl -s -w '\n%{http_code}' -X POST "$GATEWAY/api/auth/signup" \
        -H 'Content-Type: application/json' \
        -d "{\"username\":\"$username\",\"password\":\"$SEED_PASSWORD\",\"name\":\"$name\",\"slackId\":\"U0SEED$(echo "$username" | tr 'a-z' 'A-Z')\",\"affiliationName\":\"시드조직\"}")
    code=$(printf '%s' "$body" | tail -n1)

    if [ "$code" = "201" ]; then
        ok "회원가입: $username"
    elif [ "$code" = "409" ]; then
        warn "이미 존재: $username — 기존 계정을 재사용합니다"
    else
        fail "회원가입 실패: $username (HTTP $code)"
        return 1
    fi
}

# 승인 API로 상태·역할·소속을 한 번에 확정한다.
# 이 호출이 성공하면 인가(@RoleCheck), 헤더 전파, user -> hub/company Feign 검증
approve() {
    local token=$1 username=$2 role=$3 hub_id=${4:-} company_id=${5:-}

    local user_id
    user_id=$(psql_exec "SELECT id FROM user_service.p_users WHERE username='$username' LIMIT 1;")
    if [ -z "$user_id" ]; then
        fail "승인 실패: $username 사용자를 찾을 수 없습니다"
        return 1
    fi

    # 이미 승인된 계정은 엔티티가 중복 처리를 막으므로 재실행 시 건너뛴다.
    local status
    status=$(psql_exec "SELECT status FROM user_service.p_users WHERE id='$user_id';")
    if [ "$status" = "APPROVED" ]; then
        warn "이미 승인됨: $username — 승인 호출을 건너뜁니다"
        return 0
    fi

    # 역할에 따라 필요한 소속만 담는다. null을 보내면 서비스가 조합을 검증한다.
    local affiliation=""
    if [ -n "$hub_id" ]; then
        affiliation=",\"hubId\":\"$hub_id\""
    fi
    if [ -n "$company_id" ]; then
        affiliation="$affiliation,\"companyId\":\"$company_id\""
    fi

    local body code
    body=$(curl -s -w '\n%{http_code}' -X PATCH "$GATEWAY/api/users/$user_id/approval" \
        -H "Authorization: Bearer $token" \
        -H 'Content-Type: application/json' \
        -d "{\"approvalStatus\":\"APPROVED\",\"role\":\"$role\"$affiliation}")
    code=$(printf '%s' "$body" | tail -n1)

    if [ "$code" = "200" ]; then
        ok "승인 및 역할 부여: $username -> $role"
    else
        fail "승인 실패: $username -> $role (HTTP $code)"
        return 1
    fi
}

login() {
    local username=$1 password=${2:-$SEED_PASSWORD}
    curl -s -X POST "$GATEWAY/api/auth/login" \
        -H 'Content-Type: application/json' \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}" \
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

    info "[1/6] MASTER 토큰 발급"
    # 승인 API는 MASTER 역할을 부여할 수 없다(권한 상승 방지). 그래서 시드 MASTER를
    # 새로 만들지 않고, user-service 마이그레이션(V3)이 넣어둔 master01을 승인자로 쓴다.
    local token
    token=$(login "$MASTER_USERNAME" "$MASTER_PASSWORD")
    if [ -z "$token" ]; then
        fail "$MASTER_USERNAME 로그인 실패 — 이후 단계를 진행할 수 없습니다"
        warn ".env의 MASTER_PASSWORD_HASH가 비어 있으면 계정이 만들어지지 않습니다."
        warn "값을 채운 뒤 docker compose down -v 후 재기동하세요."
        exit 1
    fi
    ok "MASTER 토큰 발급 완료 ($MASTER_USERNAME)"
    printf '\n'

    info "[2/6] 사용자 회원가입"
    signup "seedhub" "시드허브관리자"
    signup "seedcomp" "시드업체관리자"
    signup "seeddeli" "시드배송담당"
    signup "seedlast" "시드업체배송"
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

    # 업체가 있어야 COMPANY_MANAGER의 소속을 확정할 수 있어 승인은 업체 생성 뒤에 한다.
    info "[5/6] 승인 및 역할·소속 부여 (승인 API)"
    approve "$token" "seedhub"  "HUB_MANAGER"      "$hub_seoul"
    approve "$token" "seedcomp" "COMPANY_MANAGER"  ""           "$co_producer"
    approve "$token" "seeddeli" "DELIVERY_MANAGER" "$hub_seoul"
    approve "$token" "seedlast" "DELIVERY_MANAGER" "$hub_busan"
    printf '\n'

    info "[6/6] 배송 담당자 등록"
    ensure_delivery_manager "$token" "seeddeli" "HUB_DELIVERY"
    ensure_delivery_manager "$token" "seedlast" "COMPANY_DELIVERY" "$hub_busan"
    printf '\n'

    info "생성 결과"
    printf '  서울특별시 센터 : %s (기준 데이터)\n' "$hub_seoul"
    printf '  부산광역시 센터 : %s (기준 데이터)\n' "$hub_busan"
    printf '  시드 생산업체   : %s\n' "$co_producer"
    printf '  시드 수령업체   : %s\n' "$co_receiver"
    printf '\n'
    printf '  계정 (비밀번호 공통: %s)\n' "$SEED_PASSWORD"
    printf '    seedhub    / HUB_MANAGER      (서울특별시 센터 소속)\n'
    printf '    seedcomp   / COMPANY_MANAGER  (시드 생산업체 소속)\n'
    printf '    seeddeli   / DELIVERY_MANAGER (서울특별시 센터 소속, 허브 배송)\n'
    printf '    seedlast   / DELIVERY_MANAGER (부산광역시 센터 소속, 업체 배송)\n'
    printf '    %s   / MASTER            (마이그레이션 기준 계정, 비밀번호 %s)\n' "$MASTER_USERNAME" "$MASTER_PASSWORD"
    printf '\n'
    printf '  토큰 발급 예시:\n'
    printf "    curl -s -X POST %s/api/auth/login -H 'Content-Type: application/json' \\\\\n" "$GATEWAY"
    printf '      -d %s\n' "'{\"username\":\"$MASTER_USERNAME\",\"password\":\"$MASTER_PASSWORD\"}'"
    printf '\n'

    if [ "$FAIL" -gt 0 ]; then
        printf '\033[31m실패 %d건, 성공 %d건\033[0m\n' "$FAIL" "$PASS"
        exit 1
    fi
    printf '\033[32m완료 — 성공 %d건\033[0m\n' "$PASS"
}

main "$@"

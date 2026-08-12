#!/usr/bin/env bash
#
# 통합 검증 시나리오
#
# 게이트웨이를 경유해 실제 요청을 보내고, 통과해야 할 것이 통과하는지와 막혀야 할 것이 막히는지를 확인한다.
#
# 이 스크립트는 거부 케이스(401/403/404 등)까지 포함해 시나리오 단위로 검증한다.
#
# 사용법:
#   ./scripts/seed-api.sh     # 먼저 시드 데이터를 만들고
#   ./scripts/verify.sh       # 검증 실행
#
#   GATEWAY=http://localhost:8080 ./scripts/verify.sh
#
# 결과 분류:
#   통과:     검증했고 기대대로 동작함
#   실패:     검증했고 기대와 다름
#   미구현:   기능이 아직 없어 검증 대상이 아님
#   확인못함: 검증하려 했으나 선행 조건이 없어 확인하지 못함
#
# 종료 코드: 실패가 하나라도 있으면 1

set -uo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
PG_CONTAINER="${PG_CONTAINER:-bonae-postgres}"
PG_USER="${PG_USER:-bonae}"
PG_DB="${PG_DB:-bonae}"
SEED_PASSWORD="${SEED_PASSWORD:-Seed1234!}"

PASS=0
FAIL=0
SKIP=0
BLOCKED=0

# 검증 과정에서 만든 업체는 실행이 끝나면 지움
# 실행마다 고유한 접두사를 붙여, 동시에 돌더라도 서로의 데이터를 지우지 않는다.
VERIFY_TAG="verify-$$-$RANDOM"

section() { printf '\n\033[36m%s\033[0m\n' "$*"; }
pass()    { printf '  \033[32m✓\033[0m %s\n' "$*"; PASS=$((PASS + 1)); }
failed()  { printf '  \033[31m✗\033[0m %s\n' "$*"; FAIL=$((FAIL + 1)); }
skip()    { printf '  \033[33m-\033[0m %s \033[2m(미구현)\033[0m\n' "$*"; SKIP=$((SKIP + 1)); }
blocked() { printf '  \033[35m?\033[0m %s \033[2m(확인 못함)\033[0m\n' "$*"; BLOCKED=$((BLOCKED + 1)); }

psql_exec() {
    docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -t -A -c "$1" 2>/dev/null
}

# uuidgen은 배포판에 따라 없을 수 있어 DB에서 생성한다(어차피 postgres에 의존하는 스크립트다).
random_uuid() {
    psql_exec "SELECT gen_random_uuid();"
}

# 상태 코드 하나를 기대하는 가장 흔한 형태의 검증
# expect_status <설명> <기대코드> <curl 인자...>
expect_status() {
    local label=$1 expected=$2
    shift 2

    local actual
    actual=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$@")

    if [ "$actual" = "$expected" ]; then
        pass "$label ($actual)"
    else
        failed "$label — 기대 $expected, 실제 $actual"
    fi
}

# 응답 본문에서 특정 값을 확인
# expect_field <설명> <jq스타일 키> <기대값> <curl 인자...>
expect_field() {
    local label=$1 key=$2 expected=$3
    shift 3

    local body actual
    body=$(curl -s --max-time 20 "$@")
    actual=$(printf '%s' "$body" | sed -n "s/.*\"$key\":\"\([^\"]*\)\".*/\1/p")

    if [ "$actual" = "$expected" ]; then
        pass "$label ($key=$actual)"
    else
        failed "$label — 기대 $key=$expected, 실제 '$actual'"
    fi
}

login() {
    curl -s --max-time 20 -X POST "$GATEWAY/api/auth/login" \
        -H 'Content-Type: application/json' \
        -d "{\"username\":\"$1\",\"password\":\"$SEED_PASSWORD\"}"
}

token_of() {
    login "$1" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

require_ready() {
    section "[사전 확인]"

    # 없는 명령 때문에 중간에 이상하게 실패하지 않도록 먼저 확인
    local missing=""
    for cmd in docker curl sed grep; do
        command -v "$cmd" >/dev/null 2>&1 || missing="$missing $cmd"
    done
    if [ -n "$missing" ]; then
        failed "필요한 명령이 없습니다:$missing"
        exit 1
    fi

    if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${PG_CONTAINER}$"; then
        failed "$PG_CONTAINER 컨테이너가 없습니다"
        exit 1
    fi
    pass "postgres 컨테이너"

    local code
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$GATEWAY/actuator/health")
    if [ "$code" != "200" ]; then
        failed "게이트웨이 응답 없음 ($GATEWAY, HTTP $code)"
        exit 1
    fi
    pass "게이트웨이 응답"

    if [ -z "$(psql_exec "SELECT 1 FROM user_service.p_users WHERE username='seedmaster' LIMIT 1;")" ]; then
        failed "시드 데이터가 없습니다. ./scripts/seed-api.sh 를 먼저 실행하세요."
        exit 1
    fi
    pass "시드 데이터 존재"
}

# ------- 레벨 1 ----------------------------------------------------------
verify_level1() {
    section "[레벨 1] 기동 통합"

    local registered
    registered=$(curl -s --max-time 10 -H 'Accept: application/json' \
        "http://localhost:8761/eureka/apps" \
        | grep -o '"name":"[A-Z-]*"' | sort -u | wc -l | tr -d ' ')

    if [ "${registered:-0}" -ge 6 ]; then
        pass "Eureka 등록 서비스 ${registered}개"
    else
        failed "Eureka 등록 부족 — ${registered}개 (6개 이상 기대)"
    fi

    # 6개 스키마에 테이블이 하나도 없는 곳이 없으면 마이그레이션이 적용된 것으로 본다.
    # (도메인 구현이 진행되며 테이블 수는 계속 늘어나므로 총량을 고정하지 않는다)
    local schemas
    schemas=$(psql_exec "SELECT count(DISTINCT table_schema) FROM information_schema.tables WHERE table_schema LIKE '%_service' AND table_type='BASE TABLE' AND table_name NOT LIKE 'flyway%';")
    if [ "${schemas:-0}" -ge 6 ]; then
        pass "마이그레이션 적용 스키마 ${schemas}개"
    else
        failed "스키마 부족 — ${schemas}개 (6개 기대)"
    fi

    local hubs
    hubs=$(psql_exec "SELECT count(*) FROM hub_service.p_hubs WHERE deleted_at IS NULL;")
    if [ "${hubs:-0}" -ge 17 ]; then
        pass "기준 허브 ${hubs}개"
    else
        failed "허브 부족 — ${hubs}개 (17개 기대)"
    fi
}

# ---- 레벨 2 ---------------------------------------------------------
verify_level2() {
    section "[레벨 2] 인증 — 통과해야 할 것"

    local token
    token=$(token_of seedmaster)
    if [ -z "$token" ]; then
        failed "MASTER 로그인 실패 — 이후 검증을 건너뜁니다"
        return 1
    fi
    pass "로그인 및 토큰 발급"

    expect_status "유효한 토큰으로 조회" 200 \
        "$GATEWAY/api/hubs" -H "Authorization: Bearer $token"

    section "[레벨 2] 인증 — 막혀야 할 것"

    expect_status "토큰 없음" 401 "$GATEWAY/api/hubs"
    expect_status "위조된 토큰" 401 \
        "$GATEWAY/api/hubs" -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.fake.sig"
    expect_status "Bearer 접두사 누락" 401 \
        "$GATEWAY/api/hubs" -H "Authorization: $token"

    section "[레벨 2] 헤더 위조 차단"

    # 게이트웨이가 클라이언트의 X-User-Id를 덮어써야 한다.
    # createdBy가 hacker면 위조가 통과한 것이다.
    local hub_id
    hub_id=$(psql_exec "SELECT id FROM hub_service.p_hubs WHERE name='서울특별시 센터' LIMIT 1;")
    expect_field "위조 X-User-Id 무시" "createdBy" "seedmaster" \
        -X POST "$GATEWAY/api/companies" \
        -H "Authorization: Bearer $token" \
        -H "X-User-Id: hacker" \
        -H 'Content-Type: application/json' \
        -d "{\"name\":\"$VERIFY_TAG-헤더위조\",\"type\":\"PRODUCER\",\"hubId\":\"$hub_id\",\"address\":\"$VERIFY_TAG-주소1\"}"

    section "[레벨 2] 로그아웃 및 토큰 재발급"

    local body access refresh
    body=$(login seedmaster)
    access=$(printf '%s' "$body" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
    refresh=$(printf '%s' "$body" | sed -n 's/.*"refreshToken":"\([^"]*\)".*/\1/p')

    expect_status "로그아웃" 204 \
        -X POST "$GATEWAY/api/auth/logout" \
        -H "Authorization: Bearer $access" \
        -H 'Content-Type: application/json' \
        -d "{\"refreshToken\":\"$refresh\"}"

    # 로그아웃한 토큰은 블랙리스트에 올라 차단되어야 한다
    expect_status "로그아웃된 토큰 차단" 401 \
        "$GATEWAY/api/hubs" -H "Authorization: Bearer $access"

    body=$(login seedmaster)
    refresh=$(printf '%s' "$body" | sed -n 's/.*"refreshToken":"\([^"]*\)".*/\1/p')

    expect_status "토큰 재발급" 200 \
        -X POST "$GATEWAY/api/auth/refresh" \
        -H 'Content-Type: application/json' \
        -d "{\"refreshToken\":\"$refresh\"}"

    # 회전된 refreshToken을 다시 쓰면 탈취로 간주해 거부한다
    expect_status "회전된 refreshToken 재사용 차단" 401 \
        -X POST "$GATEWAY/api/auth/refresh" \
        -H 'Content-Type: application/json' \
        -d "{\"refreshToken\":\"$refresh\"}"
}

#--- 레벨 3 ------------------------------------------------------
verify_level3() {
    section "[레벨 3] 권한 분기"

    local master comp
    master=$(token_of seedmaster)
    comp=$(token_of seedcomp)

    if [ -z "$comp" ]; then
        blocked "COMPANY_MANAGER 토큰 발급 실패 - 권한 검증 불가"
        return
    fi

    # COMPANY_MANAGER는 본인 업체만 수정할 수 있다.
    local other_company
    other_company=$(psql_exec "SELECT id FROM company_service.p_companies WHERE name='시드 수령업체' AND deleted_at IS NULL LIMIT 1;")

    if [ -n "$other_company" ]; then
        expect_status "COMPANY_MANAGER가 타 업체 수정 시도" 403 \
            -X PATCH "$GATEWAY/api/companies/$other_company" \
            -H "Authorization: Bearer $comp" \
            -H 'Content-Type: application/json' \
            -d '{"name":"권한없는수정"}'
    else
        blocked "타 업체 시드 데이터 없음 — 소유권 검증 불가"
    fi

    expect_status "COMPANY_MANAGER가 허브 생성 시도" 403 \
        -X POST "$GATEWAY/api/hubs" \
        -H "Authorization: Bearer $comp" \
        -H 'Content-Type: application/json' \
        -d '{"name":"무단허브","address":"어딘가 1","latitude":37.0,"longitude":127.0}'

    section "[레벨 3] 도메인 조회"

    expect_status "허브 목록" 200 "$GATEWAY/api/hubs" -H "Authorization: Bearer $master"
    expect_status "이동경로 목록" 200 "$GATEWAY/api/hub-routes" -H "Authorization: Bearer $master"
    expect_status "업체 목록" 200 "$GATEWAY/api/companies" -H "Authorization: Bearer $master"
    expect_status "배송 목록" 200 "$GATEWAY/api/deliveries" -H "Authorization: Bearer $master"
    expect_status "배송담당자 목록" 200 "$GATEWAY/api/delivery-managers" -H "Authorization: Bearer $master"

    section "[레벨 3] 잘못된 요청 처리"

    expect_status "없는 경로" 404 \
        "$GATEWAY/api/hubs/zzz/nope" -H "Authorization: Bearer $master"
    expect_status "잘못된 UUID 형식" 400 \
        "$GATEWAY/api/hubs/not-a-uuid" -H "Authorization: Bearer $master"
    expect_status "지원하지 않는 메서드" 405 \
        -X DELETE "$GATEWAY/api/hubs" -H "Authorization: Bearer $master"
    expect_status "깨진 JSON 본문" 400 \
        -X POST "$GATEWAY/api/hubs" \
        -H "Authorization: Bearer $master" \
        -H 'Content-Type: application/json' \
        -d '{broken'
    expect_status "필수값 누락" 400 \
        -X POST "$GATEWAY/api/hubs" \
        -H "Authorization: Bearer $master" \
        -H 'Content-Type: application/json' \
        -d '{"name":""}'
}

# --- 레벨 4 ------------------------------------------
verify_level4() {
    section "[레벨 4] 서비스 간 연동"

    local master hub_id
    master=$(token_of seedmaster)
    hub_id=$(psql_exec "SELECT id FROM hub_service.p_hubs WHERE name='부산광역시 센터' LIMIT 1;")

    # 업체 생성이 201이면 company -> hub Feign 호출이 성공한 것이다.
    expect_status "company → hub (허브 존재 검증)" 201 \
        -X POST "$GATEWAY/api/companies" \
        -H "Authorization: Bearer $master" \
        -H 'Content-Type: application/json' \
        -d "{\"name\":\"$VERIFY_TAG-연동\",\"type\":\"RECEIVER\",\"hubId\":\"$hub_id\",\"address\":\"$VERIFY_TAG-주소2\"}"

    # 없는 허브를 주면 Feign 응답을 도메인 예외로 변환해 404가 나와야함
    # 이 요청은 실패해야 하므로 저장되지 않는다
    expect_status "company → hub (없는 허브 거부)" 404 \
        -X POST "$GATEWAY/api/companies" \
        -H "Authorization: Bearer $master" \
        -H 'Content-Type: application/json' \
        -d "{\"name\":\"$VERIFY_TAG-연동실패\",\"type\":\"RECEIVER\",\"hubId\":\"$(random_uuid)\",\"address\":\"$VERIFY_TAG-주소3\"}"

    # 엔드포인트 존재 여부만 따로 확인한다. 빈 본문에 400이 오면 "핸들러는 있다"는 뜻이지, 연동이 동작한다는 뜻이 아니다! 통과랑 섞지 않음
    local internal_code
    internal_code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
        -X POST "http://localhost:19006/api/internal/deliveries" \
        -H 'Content-Type: application/json' -d '{}')

    case "$internal_code" in
        404) skip "order → delivery (delivery 내부 API 미구현 — 계약 확정 필요)" ;;
        400|422) blocked "order → delivery (내부 API 핸들러는 있으나 주문 경로가 막혀 연동 미검증)" ;;
        *)   blocked "order → delivery (내부 API 응답 $internal_code — 연동 미검증)" ;;
    esac

    skip "order → 재고 차감 (상품·재고 미구현)"
    skip "delivery → message 슬랙 알림 (담당자 배정 미구현)"
}

# --- 관측 ---------------------------------------------------------
verify_tracing() {
    section "[관측] 분산 추적"

    if ! command -v python3 >/dev/null 2>&1; then
        blocked "분산 추적 검증에 python3가 필요합니다"
        return
    fi

    local services
    services=$(curl -s --max-time 10 'http://localhost:9411/api/v2/services' 2>/dev/null \
        | python3 -c 'import sys,json; print(len(json.load(sys.stdin)))' 2>/dev/null)

    if [ "${services:-0}" -ge 6 ]; then
        pass "Zipkin 등록 서비스 ${services}개"
    else
        failed "Zipkin 등록 부족 — ${services:-0}개"
    fi

    # 하나의 trace 안에서 company-service가 hub-service의 업무 API를 호출했는지 본다
    local linked
    linked=$(curl -s --max-time 10 'http://localhost:9411/api/v2/traces?limit=50&lookback=300000' 2>/dev/null \
        | python3 -c '
import sys, json
from collections import defaultdict

try:
    traces = json.load(sys.stdin)
except Exception:
    print(0)
    sys.exit()

def path_of(span):
    tags = span.get("tags") or {}
    return tags.get("http.path") or tags.get("http.url") or ""

by_trace = defaultdict(list)
for trace in traces:
    for span in trace:
        by_trace[span.get("traceId")].append(span)

count = 0
for spans in by_trace.values():
    services = {(s.get("localEndpoint") or {}).get("serviceName") for s in spans}
    if "company-service" not in services:
        continue
    # hub-service가 업무 API 요청을 받은 span이 같은 trace에 있어야 한다
    for span in spans:
        name = (span.get("localEndpoint") or {}).get("serviceName")
        if name == "hub-service" and "/api/" in path_of(span):
            count += 1
            break

print(count)
' 2>/dev/null)

    if [ "${linked:-0}" -gt 0 ]; then
        pass "Feign 호출이 동일 trace로 연결됨 (company→hub ${linked}건)"
    else
        blocked "company→hub 구간이 동일 trace에서 확인되지 않음"
    fi
}

# 검증이 만든 업체를 지운다. 중간에 실패하거나 중단돼도 남지 않도록 trap으로 건다
# 이번 실행의 태그와 정확히 일치하는 행만 지우므로 시드 데이터는 건드리지 않는다.
cleanup_verify_data() {
    local removed
    removed=$(psql_exec "DELETE FROM company_service.p_companies WHERE name LIKE '${VERIFY_TAG}-%' RETURNING 1;" | wc -l | tr -d ' ')
    if [ "${removed:-0}" -gt 0 ]; then
        printf '\n검증 데이터 %s건을 정리했습니다.\n' "$removed"
    fi
}

main() {
    printf '\033[36m보내드림 물류 — 통합 검증\033[0m\n'
    printf '게이트웨이: %s\n' "$GATEWAY"

    trap cleanup_verify_data EXIT INT TERM

    require_ready
    verify_level1
    verify_level2
    verify_level3
    verify_level4
    verify_tracing

    printf '\n'
    printf '\033[36m결과\033[0m  통과 %d  실패 %d  미구현 %d  확인못함 %d\n' \
        "$PASS" "$FAIL" "$SKIP" "$BLOCKED"

    if [ "$FAIL" -gt 0 ]; then
        printf '\033[31m실패한 항목이 있습니다.\033[0m\n'
        exit 1
    fi

    # 확인 못한 항목 표시
    if [ "$BLOCKED" -gt 0 ]; then
        printf '\033[32m실패 없음\033[0m — 확인하지 못한 항목이 %d건 있습니다.\n' "$BLOCKED"
        exit 0
    fi
    printf '\033[32m모든 검증을 통과했습니다.\033[0m\n'
}

main "$@"

package com.bonae.logistics.message.presentation.docs;

/*
 * Swagger 문서에 노출할 에러 응답 예시와 설명을 모아 둔다.
 * 어노테이션 속성에는 컴파일 타임 상수만 쓸 수 있으므로 전부 static final String으로 정의한다.
 * 실제 응답 본문 형태는 common의 GlobalExceptionHandler / ErrorResponse를 따른다.
 */
public final class ApiErrorExamples {

    private ApiErrorExamples() {
    }

    /* ── 설명(description) ── */

    public static final String INVALID_INPUT_DESC =
            "요청 값 검증 실패 (필수값 누락, 길이 초과, UUID 형식 오류 등)";

    public static final String INVALID_SORT_FIELD_DESC =
            "정렬 기준이 createdAt, updatedAt이 아님";

    public static final String UNAUTHORIZED_DESC =
            "인증 필요 - 게이트웨이를 거치지 않아 X-User-Role 헤더가 없음";

    public static final String FORBIDDEN_DESC =
            "권한 없음 - 이 엔드포인트가 허용하지 않는 역할";

    public static final String SLACK_MESSAGE_NOT_FOUND_DESC =
            "메시지가 없거나 이미 삭제됨";

    public static final String SLACK_MESSAGE_NON_EDITABLE_DESC =
            "이미 발송된 메시지 - 발송 전(PENDING)만 수정 가능";

    public static final String SLACK_ID_NOT_REGISTERED_DESC =
            "수신자에게 등록된 슬랙 ID가 없음";

    public static final String UPSTREAM_ERROR_DESC =
            "연동 서비스 호출 실패 - user-service 조회 실패, 존재하지 않는 사용자 등";

    public static final String INTERNAL_ERROR_DESC =
            "서버 오류";

    /* ── 응답 본문 예시(JSON) ── */

    public static final String INVALID_INPUT_EXAMPLE = """
            {
              "code": "INVALID_INPUT",
              "message": "입력값이 올바르지 않습니다.",
              "traceId": "6f1c2d9a4b3e5f70",
              "fields": [
                { "field": "message", "reason": "메시지는 필수입니다." }
              ]
            }""";

    public static final String INVALID_SORT_FIELD_EXAMPLE = """
            {
              "code": "INVALID_SORT_FIELD",
              "message": "정렬 기준은 createdAt, updatedAt만 가능함",
              "traceId": "6f1c2d9a4b3e5f70"
            }""";

    public static final String UNAUTHORIZED_EXAMPLE = """
            {
              "code": "UNAUTHORIZED",
              "message": "인증이 필요합니다",
              "traceId": "6f1c2d9a4b3e5f70"
            }""";

    public static final String FORBIDDEN_EXAMPLE = """
            {
              "code": "FORBIDDEN",
              "message": "해당 작업을 수행할 권한이 없습니다. ",
              "traceId": "6f1c2d9a4b3e5f70"
            }""";

    public static final String SLACK_MESSAGE_NOT_FOUND_EXAMPLE = """
            {
              "code": "SLACK_MESSAGE_NOT_FOUND",
              "message": "슬렉 메시지 없음 또는 이미 삭제됨",
              "traceId": "6f1c2d9a4b3e5f70"
            }""";

    public static final String SLACK_MESSAGE_NON_EDITABLE_EXAMPLE = """
            {
              "code": "SLACK_MESSAGE_NON_EDITABLE",
              "message": "발송 전(PENDING) 메시지만 수정할 수 있습니다.",
              "traceId": "6f1c2d9a4b3e5f70"
            }""";

    public static final String SLACK_ID_NOT_REGISTERED_EXAMPLE = """
            {
              "code": "SLACK_ID_NOT_REGISTERED",
              "message": "수신자에게 등록된 슬랙 ID가 없습니다.",
              "traceId": "6f1c2d9a4b3e5f70"
            }""";

    public static final String UPSTREAM_ERROR_EXAMPLE = """
            {
              "code": "UPSTREAM_ERROR",
              "message": "연동 서비스 호출에 실패했습니다.",
              "traceId": "6f1c2d9a4b3e5f70"
            }""";

    public static final String INTERNAL_ERROR_EXAMPLE = """
            {
              "code": "INTERNAL_ERROR",
              "message": "서버 오류가 발생했습니다. ",
              "traceId": "6f1c2d9a4b3e5f70"
            }""";
}

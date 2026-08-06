package com.bonae.logistics.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode{
    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_SORT_FIELD(HttpStatus.BAD_REQUEST, "정렬 기준은 createdAt, updatedAt만 가능함"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "해당 작업을 수행할 권한이 없습니다. "),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다. "),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "일시적으로 서비스를 사용할 수 없습니다. "),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. "),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "잘못된 상태 변경입니다."),

    // 유저/인증
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USERNAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    USER_NOT_APPROVED(HttpStatus.CONFLICT, "승인 대기 중이거나 거절된 계정입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다. 다시 로그인해주세요."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "리프레시 토큰을 찾을 수 없습니다."),
    USER_ALREADY_PROCESSED(HttpStatus.CONFLICT ,"이미 처리된 가입 요청입니다"),

    // 허브
    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "허브를 찾을 수 없습니다. "),
    HUB_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "허브 라우터를 찾을 수 없습니다."),
    HUB_ROUTE_DUPLICATED(HttpStatus.CONFLICT, "중복된 경로입니다."),
    HUB_NAME_DUPLICATED(HttpStatus.CONFLICT, "중복된 허브명입니다."),
    HUB_ADDRESS_DUPLICATED(HttpStatus.CONFLICT, "중복된 허브 주소입니다."),
    INVALID_HUB_ROUTE_DISTANCE(HttpStatus.BAD_REQUEST, "이동 거리는 0보다 커야 합니다."),
    INVALID_HUB_ROUTE_DURATION(HttpStatus.BAD_REQUEST, "소요 시간은 0보다 커야 합니다."),
    SAME_HUB_ROUTE_ENDPOINTS(HttpStatus.BAD_REQUEST, "출발 허브와 도착 허브는 달라야 합니다."),

    // 업체/상품/인벤토리
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "업체를 찾을 수 없습니다. "),
    INVALID_COMPANY_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 업체 유형입니다."),
    COMPANY_DUPLICATED(HttpStatus.CONFLICT, "이미 동일 명의 업체가 존재합니다."),
    COMPANY_ADDRESS_DUPLICATED(HttpStatus.CONFLICT, "등록된 주소에 이미 업체가 존재합니다."),

    // 상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다. "),
    PRODUCT_DUPLICATED(HttpStatus.CONFLICT, "상품이 이미 존재합니다."),
    INVALID_PRICE(HttpStatus.BAD_REQUEST, "허용되지 않는 가격입니다."),

    // 인벤토리
    STOCK_SHORTAGE(HttpStatus.CONFLICT, "재고가 부족합니다. "),
    INVENTORY_DUPLICATED(HttpStatus.CONFLICT, "중복된 재고입니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "허용되지 않는 수량입니다."),
    INVALID_INVENTORY_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 재고 유형입니다."),
    INVENTORY_VERSION_CONFLICT(HttpStatus.CONFLICT, "동시 수정으로 데이터 충돌"),
    DUPLICATE_INVENTORY_UPDATE(HttpStatus.CONFLICT, "동일한 재고 변경에 중복 처리"),

    // 주문
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "주문 생성에 실패했습니다. "),
    ORDER_ALREADY_DELETED(HttpStatus.NOT_FOUND, "이미 삭제된 주문입니다"),
    INVALID_DUE_DATE(HttpStatus.BAD_REQUEST, "생성할 수 없는 주문 날짜입니다"),
    INVALID_UUID_FORMAT(HttpStatus.BAD_REQUEST, "Path variable이 UUID 형식이 아닙니다."),
    ORDER_STATUS_CONFLICT(HttpStatus.CONFLICT, "이미 배송이 시작되어 주문을 수정할 수 없습니다."),
    ORDER_ALREADY_DELIVERED(HttpStatus.CONFLICT, "이미 배송완료되어 주문을 취소할 수 없습니다."),

    // 배송
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "배송을 찾을 수 없습니다. "),
    DELIVERY_MANAGER_NOT_AVAILABLE(HttpStatus.CONFLICT, "배정 가능한 배송 담당자가 없습니다. "),
    DELIVERY_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "이미 배송 담당자가 할당되었습니다."),
    DELIVERY_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 배송 완료되었습니다."),
    DELIVERY_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 배송입니다."),
    DELIVERY_MANAGER_ALREADY_DELETED(HttpStatus.CONFLICT, "이미 삭제된 배송 담당자입니다."),
    INVALID_DELIVERY_RECEIVER_NAME(HttpStatus.BAD_REQUEST, "수령인명은 비어 있을 수 없고 50자를 초과할 수 없습니다."),
    INVALID_DELIVERY_RECEIVER_SLACK_ID(HttpStatus.BAD_REQUEST, "수령인 Slack ID는 비어 있을 수 없고 50자를 초과할 수 없습니다."),
    INVALID_DELIVERY_ADDRESS(HttpStatus.BAD_REQUEST, "배송지 주소는 비어 있을 수 없고 255자를 초과할 수 없습니다."),
    INVALID_DELIVERY_ASSIGNMENT_SEQUENCE(HttpStatus.BAD_REQUEST, "배송 배정 순번은 1 이상이어야 합니다."),
    INVALID_DELIVERY_ASSIGNMENT_REASON(HttpStatus.BAD_REQUEST, "배송 배정 사유는 255자를 초과할 수 없습니다."),
    INVALID_DELIVERY_MANAGER_SEQUENCE(HttpStatus.BAD_REQUEST, "배송 담당자 순번은 0 이상이어야 합니다."),
    INVALID_DELIVERY_MANAGER_HUB_MAPPING(HttpStatus.BAD_REQUEST, "배송 담당자 타입과 소속 허브 ID 조합이 올바르지 않습니다."),
    INVALID_DELIVERY_ROUTE_SEQUENCE(HttpStatus.BAD_REQUEST, "배송 경로 순번은 1 이상이어야 합니다."),
    INVALID_DELIVERY_ROUTE_DISTANCE(HttpStatus.BAD_REQUEST, "배송 경로 예상 거리는 0 이상이며 소수 둘째 자리까지 허용됩니다."),
    INVALID_DELIVERY_ROUTE_DURATION(HttpStatus.BAD_REQUEST, "배송 경로 예상 소요 시간은 0 이상이어야 합니다."),

    // 슬렉/AI
    SLACK_SEND_FAILED(HttpStatus.BAD_GATEWAY, "슬렉 메시지 발송에 실패했습니다."),
    SLACK_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "슬렉 메시지 없음 또는 이미 삭제됨"),
    SLACK_MESSAGE_ALREADY_SENT(HttpStatus.CONFLICT, "이미 발송 완료된 메시지 수정 시도"),
    AI_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "AI 응답 생성에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}

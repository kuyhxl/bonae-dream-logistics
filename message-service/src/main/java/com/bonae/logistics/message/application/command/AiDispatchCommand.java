package com.bonae.logistics.message.application.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/* 배송 서비스가 넘겨주는 주문·경로 정보. presentation DTO를 application까지 끌고 오지 않기 위한 커맨드. */
public record AiDispatchCommand(
        UUID orderId,
        String orderNo,
        String ordererName,
        String ordererSlackId,
        String productName,
        Integer quantity,
        LocalDateTime dueDate,
        String requestNote,
        String originHubName,
        List<String> waypointHubNames,
        String destinationAddress,
        String managerSlackId
) {
    /* 경유지는 선택값이다. null 방어를 호출부마다 하지 않도록 여기서 정규화한다. */
    @Override
    public List<String> waypointHubNames() {
        return waypointHubNames == null ? List.of() : waypointHubNames;
    }
}
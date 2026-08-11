package com.bonae.logistics.message.application.deadline;

import com.bonae.logistics.message.application.command.AiDispatchCommand;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/* 요구사항의 "슬랙 발송 메시지 예시" 포맷 */
@Component
public class DispatchNotificationFormatter {

    private static final DateTimeFormatter ORDERED_AT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DEADLINE =
            DateTimeFormatter.ofPattern("M월 d일 a h시", Locale.KOREAN);

    private static final String DASH = "-";

    public String format(AiDispatchCommand command, LocalDateTime deadline, LocalDateTime notifiedAt) {
        return """
                주문 번호 : %s
                주문자 정보 : %s / %s
                주문 시간 : %s
                상품 정보 : %s %d박스
                요청 사항 : %s
                발송지 : %s
                경유지 : %s
                도착지 : %s

                위 내용을 기반으로 도출된 최종 발송 시한은 %s 입니다."""
                .formatted(
                        orDash(command.orderNo()),
                        command.ordererName(),
                        command.ordererSlackId(),
                        notifiedAt.format(ORDERED_AT),
                        command.productName(),
                        command.quantity(),
                        orDash(command.requestNote()),
                        command.originHubName(),
                        command.waypointHubNames().isEmpty()
                                ? DASH : String.join(", ", command.waypointHubNames()),
                        command.destinationAddress(),
                        deadline.format(DEADLINE)
                );
    }

    private String orDash(String value) {
        return (value == null || value.isBlank()) ? DASH : value;
    }
}
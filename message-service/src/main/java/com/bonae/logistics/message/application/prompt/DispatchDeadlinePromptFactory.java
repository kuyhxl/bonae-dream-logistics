package com.bonae.logistics.message.application.prompt;

import com.bonae.logistics.message.application.command.AiDispatchCommand;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class DispatchDeadlinePromptFactory {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public String create(AiDispatchCommand c) {
        return """
                너는 물류 발송 시한을 계산하는 어시스턴트다.
                아래 주문·경로 정보를 보고 최종 발송 시한을 계산해라.

                [계산 조건]
                - 배송 담당자 근무시간은 09:00~18:00 이며, 발송 시한은 반드시 이 시간 범위 안이어야 한다.
                - 발송 시한은 납기 일시보다 반드시 이전이어야 한다.
                - 허브 간 이동 시간과 최종 도착지까지의 배송 시간을 감안해 여유 있게 잡는다.

                [주문 정보]
                주문번호: %s
                주문자: %s
                상품: %s %d개
                납기 일시: %s
                요청사항: %s

                [경로 정보]
                발송지 허브: %s
                경유지 허브: %s
                도착지 주소: %s
                허브 간 총 예상 소요시간: %d분

                [출력 형식]
                다른 설명 없이 아래 JSON 한 줄만 출력해라.
                {"finalDispatchDeadline": "yyyy-MM-ddTHH:mm:ss"}
                """.formatted(
                orDash(c.orderNo()),
                c.ordererName(),
                c.productName(), c.quantity(),
                c.dueDate().format(ISO),
                orDash(c.requestNote()),
                c.originHubName(),
                c.waypointHubNames().isEmpty() ? "없음" : String.join(", ", c.waypointHubNames()),
                c.destinationAddress(),
                c.totalDurationMin()
        );
    }

    private String orDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
}
package com.bonae.logistics.message.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
@Table(name = "p_ai_dispatch_logs", schema = "message_service")
public class AiDispatchLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "request_content", nullable = false)
    private String requestContent;

    @Column(name = "response_content", nullable = false)
    private String responseContent;

    @Column(name = "final_dispatch_deadline", nullable = false)
    private LocalDateTime finalDispatchDeadline;

    @Column(name = "slack_message_id")
    private UUID slackMessageId;
}

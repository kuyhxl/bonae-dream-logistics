package com.bonae.logistics.message.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(name = "p_slack_messages", schema = "message_service")
public class SlackMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "receiver_slack_id", nullable = false, length = 100)
    private String receiverSlackId;

    @Column(name = "message", nullable = false)
    private String message;

    @Builder.Default
    @Column(name = "send_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SendStatus sendStatus = SendStatus.PENDING;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "source_type", length = 20)
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;
}

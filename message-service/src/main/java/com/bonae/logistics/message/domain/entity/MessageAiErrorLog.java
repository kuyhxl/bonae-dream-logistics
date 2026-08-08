package com.bonae.logistics.message.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
@Table(name = "p_message_error_logs", schema = "message_service")
public class MessageAiErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "error_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ErrorType errorType;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Builder.Default
    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo = 1;

    @Column(name = "error_code", nullable = false, length = 50)
    private String errorCode;

    @Column(name = "error_message", nullable = false)
    private String errorMessage;

    @Column(name = "trace_id", length = 50)
    private String traceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

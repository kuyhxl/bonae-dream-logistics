package com.bonae.logistics.message.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "send_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SendStatus sendStatus;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "source_type", length = 20)
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private SlackMessage(String receiverSlackId, String message, SourceType sourceType) {
        this.receiverSlackId = receiverSlackId;
        this.message = message;
        this.sourceType = sourceType;
        this.sendStatus = SendStatus.PENDING;
        this.retryCount = 0;
    }

    /* 발송 전 상태로만 생성된다. SUCCESS 상태의 레코드는 이 경로로 만들 수 없다. */
    public static SlackMessage pending(String receiverSlackId, String message, SourceType sourceType) {
        return new SlackMessage(receiverSlackId, message, sourceType);
    }

    public void markSuccess(LocalDateTime sentAt) {
        this.sendStatus = SendStatus.SUCCESS;
        this.sentAt = sentAt;
    }

    /* SUCCESS가 아니면 sent_at은 반드시 null이다. */
    public void markFailed(int retryCount) {
        this.sendStatus = SendStatus.FAILED;
        this.retryCount = retryCount;
        this.sentAt = null;
    }

    /*
     * PENDING 상태에서만 수정할 수 있다.
     * 이미 슬랙으로 나간 메시지는 내용을 바꿔도 슬랙에 반영되지 않는다.
     */
    public void update(String receiverSlackId, String message) {
        if (this.sendStatus != SendStatus.PENDING) {
            throw new BusinessException(ErrorCode.SLACK_MESSAGE_NON_EDITABLE);
        }
        if (StringUtils.hasText(receiverSlackId)) {
            this.receiverSlackId = receiverSlackId;
        }
        if (StringUtils.hasText(message)) {
            this.message = message;
        }
    }
}

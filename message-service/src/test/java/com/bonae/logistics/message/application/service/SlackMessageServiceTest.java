package com.bonae.logistics.message.application.service;

import com.bonae.logistics.message.domain.entity.SendStatus;
import com.bonae.logistics.message.domain.entity.SlackMessage;
import com.bonae.logistics.message.domain.entity.SourceType;
import com.bonae.logistics.message.infrastructure.slack.SlackClient;
import com.bonae.logistics.message.infrastructure.slack.SlackSendResult;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SlackMessageServiceTest {

    private SlackMessageStore store;
    private SlackClient slackClient;
    private SlackMessageService service;

    @BeforeEach
    void setUp() {
        store = mock(SlackMessageStore.class);
        slackClient = mock(SlackClient.class);
        service = new SlackMessageService(store, slackClient, mock(Tracer.class));
    }

    @Test
    @DisplayName("발송 성공 시 PENDING 저장 후 성공 처리된다")
    void sendSuccess() {
        SlackMessage pending = SlackMessage.pending("U01", "hi", SourceType.USER);
        when(store.savePending(any(), any(), any())).thenReturn(pending);
        when(slackClient.send("U01", "hi")).thenReturn(SlackSendResult.ok());
        when(store.markSuccess(any())).thenReturn(pending);

        service.send("U01", "hi", SourceType.USER);

        verify(store).savePending("U01", "hi", SourceType.USER);
        verify(store).markSuccess(any());
        verify(store, never()).markFailed(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("발송 실패해도 예외를 던지지 않고 FAILED로 기록한다")
    void sendFailureIsRecorded() {
        SlackMessage pending = SlackMessage.pending("U01", "hi", SourceType.USER);
        when(store.savePending(any(), any(), any())).thenReturn(pending);
        when(slackClient.send(any(), any())).thenReturn(SlackSendResult.failure("channel_not_found", 1));
        when(store.markFailed(any(), any(), anyInt(), any())).thenReturn(pending);

        service.send("U01", "hi", SourceType.USER);

        verify(store).markFailed(any(), eq("channel_not_found"), eq(1), any());
    }

    @Test
    @DisplayName("markFailed는 sentAt을 남기지 않는다")
    void failedHasNoSentAt() {
        SlackMessage m = SlackMessage.pending("U01", "hi", SourceType.USER);
        m.markFailed(2);

        assertThat(m.getSendStatus()).isEqualTo(SendStatus.FAILED);
        assertThat(m.getSentAt()).isNull();
        assertThat(m.getRetryCount()).isEqualTo(2);
    }
}
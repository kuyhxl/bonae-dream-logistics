package com.bonae.logistics.message.application.service;

import com.bonae.logistics.message.application.command.AiDispatchCommand;
import com.bonae.logistics.message.application.deadline.DispatchDeadlineCalculator;
import com.bonae.logistics.message.application.deadline.DispatchDeadlineProperties;
import com.bonae.logistics.message.application.deadline.DispatchNotificationFormatter;
import com.bonae.logistics.message.application.dto.AiDispatchResult;
import com.bonae.logistics.message.application.prompt.DispatchDeadlinePromptFactory;
import com.bonae.logistics.message.application.service.AiDispatchStore.SavedDispatch;
import com.bonae.logistics.message.domain.entity.AiDispatchLog;
import com.bonae.logistics.message.domain.entity.SlackMessage;
import com.bonae.logistics.message.domain.entity.SourceType;
import com.bonae.logistics.message.infrastructure.ai.AiDeadlineClient;
import com.bonae.logistics.message.infrastructure.ai.AiDeadlineResult;
import com.bonae.logistics.message.infrastructure.slack.SlackClient;
import com.bonae.logistics.message.infrastructure.slack.SlackSendResult;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiDispatchServiceTest {

    private AiDeadlineClient aiDeadlineClient;
    private AiDispatchStore aiDispatchStore;
    private SlackMessageStore slackMessageStore;
    private SlackClient slackClient;
    private AiDispatchService service;

    private static final LocalDateTime DUE_DATE = LocalDateTime.of(2026, 8, 12, 15, 0);

    @BeforeEach
    void setUp() {
        aiDeadlineClient = mock(AiDeadlineClient.class);
        aiDispatchStore = mock(AiDispatchStore.class);
        slackMessageStore = mock(SlackMessageStore.class);
        slackClient = mock(SlackClient.class);

        DispatchDeadlineProperties properties = new DispatchDeadlineProperties(9, 18, 120, 60);

        service = new AiDispatchService(
                new DispatchDeadlinePromptFactory(),
                aiDeadlineClient,
                new DispatchDeadlineCalculator(properties),
                new DispatchNotificationFormatter(),
                aiDispatchStore,
                slackMessageStore,
                slackClient,
                mock(Tracer.class));

        when(aiDispatchStore.saveLogWithPendingMessage(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> new SavedDispatch(
                        AiDispatchLog.builder()
                                .id(UUID.randomUUID())
                                .orderId(invocation.getArgument(0))
                                .requestContent(invocation.getArgument(1))
                                .responseContent(invocation.getArgument(2))
                                .finalDispatchDeadline(invocation.getArgument(3))
                                .build(),
                        SlackMessage.pending(invocation.getArgument(4), invocation.getArgument(5), SourceType.SYSTEM)));
    }

    @Test
    @DisplayName("AI 성공 시 AI가 계산한 시한이 그대로 사용된다")
    void useAiDeadline() {
        LocalDateTime aiDeadline = LocalDateTime.of(2026, 8, 10, 9, 0);
        when(aiDeadlineClient.generate(any())).thenReturn(AiDeadlineResult.ok(aiDeadline, "{}"));
        when(slackClient.send(any(), any())).thenReturn(SlackSendResult.ok());

        AiDispatchResult result = service.calculateAndNotify(command());

        assertThat(result.finalDispatchDeadline()).isEqualTo(aiDeadline);
        verify(aiDispatchStore, never()).saveAiErrorLog(any(), any(), anyInt(), any(), any());
        verify(slackMessageStore).markSuccess(any());
    }

    @Test
    @DisplayName("AI 실패 시 산술 폴백값을 쓰고 오류 로그를 남기되 알림은 정상 발송한다")
    void fallbackWhenAiFails() {
        when(aiDeadlineClient.generate(any())).thenReturn(AiDeadlineResult.failure("response_parse_error", 2));
        when(slackClient.send(any(), any())).thenReturn(SlackSendResult.ok());

        AiDispatchResult result = service.calculateAndNotify(command());

        // 720(허브) + 120(라스트마일) + 60(여유) = 900분 역산 = 8/12 00:00 -> 근무시간 보정으로 8/11 18:00
        assertThat(result.finalDispatchDeadline()).isEqualTo(LocalDateTime.of(2026, 8, 11, 18, 0));
        verify(aiDispatchStore).saveAiErrorLog(any(), eq("response_parse_error"), eq(2), any(), any());
        verify(slackClient).send(eq("U08MANAGER01"), any());
    }

    @Test
    @DisplayName("슬랙 발송이 실패해도 예외를 던지지 않고 FAILED로 기록한다")
    void slackFailureIsRecorded() {
        when(aiDeadlineClient.generate(any()))
                .thenReturn(AiDeadlineResult.ok(LocalDateTime.of(2026, 8, 10, 9, 0), "{}"));
        when(slackClient.send(any(), any())).thenReturn(SlackSendResult.failure("channel_not_found", 1));

        service.calculateAndNotify(command());

        verify(slackMessageStore).markFailed(any(), eq("channel_not_found"), eq(1), any());
        verify(slackMessageStore, never()).markSuccess(any());
    }

    private AiDispatchCommand command() {
        return new AiDispatchCommand(
                UUID.randomUUID(), "ORD-20260805-0001", "김말숙", "U08ORDER001",
                "마른 오징어", 50, DUE_DATE, "8월 12일 3시까지는 보내주세요!",
                "경기 북부 센터", List.of("대전광역시 센터", "부산광역시 센터"),
                "부산시 사하구 낙동대로 1번길 1 해산물월드", 720, "U08MANAGER01");
    }

    @Test
    @DisplayName("AI가 납기일 이후 시한을 반환하면 폴백값을 사용한다")
    void rejectDeadlineAfterDueDate() {
        when(aiDeadlineClient.generate(any()))
                .thenReturn(AiDeadlineResult.ok(DUE_DATE.plusHours(1), "{}"));
        when(slackClient.send(any(), any())).thenReturn(SlackSendResult.ok());

        AiDispatchResult result = service.calculateAndNotify(command());

        assertThat(result.finalDispatchDeadline()).isEqualTo(LocalDateTime.of(2026, 8, 11, 18, 0));
        verify(aiDispatchStore).saveAiErrorLog(any(), eq("invalid_ai_deadline"), eq(1), any(), any());
    }

    @Test
    @DisplayName("AI가 근무시간 밖 시한을 반환하면 폴백값을 사용한다")
    void rejectDeadlineOutsideWorkingHour() {
        when(aiDeadlineClient.generate(any()))
                .thenReturn(AiDeadlineResult.ok(LocalDateTime.of(2026, 8, 10, 22, 0), "{}"));
        when(slackClient.send(any(), any())).thenReturn(SlackSendResult.ok());

        AiDispatchResult result = service.calculateAndNotify(command());

        assertThat(result.finalDispatchDeadline()).isEqualTo(LocalDateTime.of(2026, 8, 11, 18, 0));
        verify(aiDispatchStore).saveAiErrorLog(any(), eq("invalid_ai_deadline"), eq(1), any(), any());
    }
}
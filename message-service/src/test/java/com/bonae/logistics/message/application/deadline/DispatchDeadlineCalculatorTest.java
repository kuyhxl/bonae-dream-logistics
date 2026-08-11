package com.bonae.logistics.message.application.deadline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchDeadlineCalculatorTest {

    private final DispatchDeadlineCalculator calculator =
            new DispatchDeadlineCalculator(new DispatchDeadlineProperties(9, 18, 120, 60));

    @Test
    @DisplayName("근무시간 이전으로 계산되면 전날 퇴근 시각으로 당긴다")
    void beforeWorkingHour() {
        LocalDateTime result = calculator.toWorkingHour(LocalDateTime.of(2026, 8, 12, 3, 0));
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 8, 11, 18, 0));
    }

    @Test
    @DisplayName("근무시간 이후로 계산되면 당일 퇴근 시각으로 당긴다")
    void afterWorkingHour() {
        LocalDateTime result = calculator.toWorkingHour(LocalDateTime.of(2026, 8, 12, 22, 30));
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 8, 12, 18, 0));
    }

    @Test
    @DisplayName("총 소요시간에 라스트마일과 여유를 더해 납기일에서 역산한다")
    void subtractsTotalDuration() {
        // 120(허브) + 120(라스트마일) + 60(여유) = 300분 = 5시간
        LocalDateTime result = calculator.fallback(LocalDateTime.of(2026, 8, 20, 15, 0), 120);
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 8, 20, 10, 0));
    }

    @Test
    @DisplayName("소요시간이 길수록 발송 시한이 앞당겨진다")
    void longerDurationMeansEarlierDeadline() {
        LocalDateTime dueDate = LocalDateTime.of(2026, 8, 20, 15, 0);
        assertThat(calculator.fallback(dueDate, 600)).isBefore(calculator.fallback(dueDate, 120));
    }
}
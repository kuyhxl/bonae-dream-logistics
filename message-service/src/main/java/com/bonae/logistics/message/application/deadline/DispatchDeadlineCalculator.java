package com.bonae.logistics.message.application.deadline;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/*
 * AI 호출·파싱이 실패했을 때 쓰는 산술 폴백.
 * 납기일에서 총 예상 소요시간을 역산하고, 배송 담당자 근무시간(09:00~18:00) 안으로 보정한다.
 */
@Component
@RequiredArgsConstructor
public class DispatchDeadlineCalculator {

    private final DispatchDeadlineProperties properties;

    /* 배송 서비스가 허브 경로 조회로 받은 총 소요시간을 납기일에서 역산한다. */
    public LocalDateTime fallback(LocalDateTime dueDate, int totalDurationMin) {
        int totalMinutes = totalDurationMin
                + properties.lastMileMinutes()
                + properties.bufferMinutes();

        return toWorkingHour(dueDate.minusMinutes(totalMinutes));
    }

    /* 근무시간 이전이면 전날 퇴근 시각으로, 이후면 당일 퇴근 시각으로 당긴다(늦추지 않는다). */
    LocalDateTime toWorkingHour(LocalDateTime time) {
        LocalDateTime start = time.toLocalDate().atTime(properties.workingHourStart(), 0);
        LocalDateTime end = time.toLocalDate().atTime(properties.workingHourEnd(), 0);

        if (time.isBefore(start)) {
            return time.toLocalDate().minusDays(1).atTime(properties.workingHourEnd(), 0);
        }
        if (time.isAfter(end)) {
            return end;
        }
        return time;
    }
}
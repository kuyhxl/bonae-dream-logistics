package com.bonae.logistics.message.application.deadline;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

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

    /*
     * AI 응답은 신뢰할 수 없는 입력이다. 프롬프트로 지시한 조건을 코드로 다시 검증한다.
     * 위반하면 보정하지 않고 산술 폴백으로 대체한다(납기 역전은 보정으로 해결되지 않는다).
     */
    public boolean isAcceptable(LocalDateTime deadline, LocalDateTime dueDate) {
        if (deadline == null || !deadline.isBefore(dueDate)) {
            return false;
        }
        LocalTime time = deadline.toLocalTime();
        return !time.isBefore(LocalTime.of(properties.workingHourStart(), 0))
                && !time.isAfter(LocalTime.of(properties.workingHourEnd(), 0));
    }
}
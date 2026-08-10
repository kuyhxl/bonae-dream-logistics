package com.bonae.logistics.message.application.deadline;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dispatch")
public record DispatchDeadlineProperties(
        int workingHourStart,
        int workingHourEnd,
        int lastMileMinutes,
        int bufferMinutes
) {
}

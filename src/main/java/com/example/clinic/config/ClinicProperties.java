package com.example.clinic.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clinic")
public record ClinicProperties(
        String timezone,
        int cancellationCutoffHours,
        int bookingHorizonDays,
        boolean showQueuePosition,
        int walkInPriorityMinutes,
        boolean seedDemo) {

    public ZoneId zone() {
        return ZoneId.of(timezone);
    }
}

package com.flashkart.order.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generates unique order numbers.
 * Format: ORD-YYYYMMDD-HHMMSS-{shortUUID}
 */
@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss")
            .withZone(ZoneId.systemDefault());

    public String generate() {
        Instant now = Instant.now();
        String date = DATE_FORMATTER.format(now);
        String time = TIME_FORMATTER.format(now);
        String shortUuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("ORD-%s-%s-%s", date, time, shortUuid);
    }
}

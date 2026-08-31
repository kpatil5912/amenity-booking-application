package com.amenityhub.resource.dto;

import java.time.Instant;

public record SlotResponse(
        Long id,
        Long resourceId,
        Instant startTime,
        Instant endTime,
        int totalCapacity,
        int bookedCount,
        int remainingCapacity,
        boolean available) {
}

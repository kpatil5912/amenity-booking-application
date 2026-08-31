package com.amenityhub.resource.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateSlotRequest(
        @NotNull @Future Instant startTime,
        @NotNull @Future Instant endTime,
        @Min(1) int totalCapacity) {
}

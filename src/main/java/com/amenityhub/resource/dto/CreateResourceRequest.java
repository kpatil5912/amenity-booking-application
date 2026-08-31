package com.amenityhub.resource.dto;

import com.amenityhub.resource.entity.ResourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateResourceRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @NotNull ResourceType resourceType,
        @Min(1) @Max(1000) int capacity,
        @Min(0) @Max(168) int cancellationWindowHours) {
}

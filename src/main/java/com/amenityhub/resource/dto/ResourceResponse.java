package com.amenityhub.resource.dto;

import com.amenityhub.resource.entity.ResourceType;

public record ResourceResponse(
        Long id,
        String name,
        String description,
        ResourceType resourceType,
        int capacity,
        int cancellationWindowHours,
        boolean active) {
}

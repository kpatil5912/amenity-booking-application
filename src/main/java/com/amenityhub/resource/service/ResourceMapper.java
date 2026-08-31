package com.amenityhub.resource.service;
import com.amenityhub.resource.entity.Resource;
import com.amenityhub.resource.entity.AvailabilitySlot;

import com.amenityhub.resource.dto.ResourceResponse;
import com.amenityhub.resource.dto.SlotResponse;

public final class ResourceMapper {

    private ResourceMapper() {
    }

    public static ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getResourceType(),
                resource.getCapacity(),
                resource.getCancellationWindowHours(),
                resource.isActive());
    }

    public static SlotResponse toResponse(AvailabilitySlot slot) {
        return new SlotResponse(
                slot.getId(),
                slot.getResource().getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getTotalCapacity(),
                slot.getBookedCount(),
                slot.remainingCapacity(),
                slot.hasFreeCapacity());
    }
}

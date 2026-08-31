package com.amenityhub.resource.service;
import com.amenityhub.resource.entity.Resource;
import com.amenityhub.resource.entity.AvailabilitySlot;
import com.amenityhub.resource.entity.ResourceType;
import com.amenityhub.resource.repository.ResourceRepository;
import com.amenityhub.resource.repository.AvailabilitySlotRepository;

import com.amenityhub.common.exception.BusinessRuleException;
import com.amenityhub.common.exception.ConflictException;
import com.amenityhub.common.exception.NotFoundException;
import com.amenityhub.resource.dto.CreateResourceRequest;
import com.amenityhub.resource.dto.CreateSlotRequest;
import com.amenityhub.resource.dto.ResourceResponse;
import com.amenityhub.resource.dto.SlotResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final AvailabilitySlotRepository slotRepository;

    public ResourceService(
            ResourceRepository resourceRepository,
            AvailabilitySlotRepository slotRepository) {
        this.resourceRepository = resourceRepository;
        this.slotRepository = slotRepository;
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listActiveResources() {
        return resourceRepository.findByActiveTrue().stream()
                .map(ResourceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResource(Long resourceId) {
        return ResourceMapper.toResponse(requireResource(resourceId));
    }

    /**
     * Returns the slots for a resource on a given UTC calendar date.
     */
    @Transactional(readOnly = true)
    public List<SlotResponse> getAvailability(Long resourceId, LocalDate date) {
        requireResource(resourceId);
        Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return slotRepository
                .findByResourceIdAndStartTimeBetweenOrderByStartTime(resourceId, from, to).stream()
                .map(ResourceMapper::toResponse)
                .toList();
    }

    @Transactional
    public ResourceResponse createResource(CreateResourceRequest request) {
        Resource resource = new Resource();
        resource.setName(request.name());
        resource.setDescription(request.description());
        resource.setResourceType(request.resourceType());
        resource.setCapacity(request.capacity());
        resource.setCancellationWindowHours(request.cancellationWindowHours());
        resource.setActive(true);
        return ResourceMapper.toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public SlotResponse createSlot(Long resourceId, CreateSlotRequest request) {
        Resource resource = requireResource(resourceId);

        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessRuleException("Slot end time must be after start time");
        }

        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setResource(resource);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setTotalCapacity(request.totalCapacity());
        slot.setBookedCount(0);

        try {
            return ResourceMapper.toResponse(slotRepository.saveAndFlush(slot));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "A slot with the same time window already exists for this resource");
        }
    }

    private Resource requireResource(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found: " + resourceId));
    }
}

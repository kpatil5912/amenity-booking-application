package com.amenityhub.resource.controller;
import com.amenityhub.resource.service.ResourceService;

import com.amenityhub.resource.dto.CreateResourceRequest;
import com.amenityhub.resource.dto.CreateSlotRequest;
import com.amenityhub.resource.dto.ResourceResponse;
import com.amenityhub.resource.dto.SlotResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public List<ResourceResponse> listResources() {
        return resourceService.listActiveResources();
    }

    @GetMapping("/{resourceId}")
    public ResourceResponse getResource(@PathVariable Long resourceId) {
        return resourceService.getResource(resourceId);
    }

    @GetMapping("/{resourceId}/availability")
    public List<SlotResponse> getAvailability(
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return resourceService.getAvailability(resourceId, date);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ResourceResponse> createResource(
            @Valid @RequestBody CreateResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.createResource(request));
    }

    @PostMapping("/{resourceId}/slots")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SlotResponse> createSlot(
            @PathVariable Long resourceId,
            @Valid @RequestBody CreateSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.createSlot(resourceId, request));
    }
}

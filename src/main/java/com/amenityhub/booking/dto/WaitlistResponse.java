package com.amenityhub.booking.dto;

public record WaitlistResponse(
        Long waitlistEntryId,
        Long slotId,
        int position,
        String message) {
}

package com.amenityhub.booking.dto;

import com.amenityhub.booking.entity.Booking;
import com.amenityhub.booking.entity.BookingStatus;
import java.time.Instant;

public record BookingResponse(
        Long id,
        String bookingReference,
        Long slotId,
        Long resourceId,
        Instant slotStartTime,
        Instant slotEndTime,
        BookingStatus status,
        Instant createdAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getSlot().getId(),
                booking.getSlot().getResource().getId(),
                booking.getSlot().getStartTime(),
                booking.getSlot().getEndTime(),
                booking.getStatus(),
                booking.getCreatedAt());
    }
}

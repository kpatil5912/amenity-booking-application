package com.amenityhub.booking.dto;

import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
        @NotNull Long slotId,
        /**
         * When true, if the slot is full the caller is added to the waitlist
         * instead of receiving a "slot full" error.
         */
        boolean joinWaitlistIfFull) {
}

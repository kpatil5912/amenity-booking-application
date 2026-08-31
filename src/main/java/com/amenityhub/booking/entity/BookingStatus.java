package com.amenityhub.booking.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Booking lifecycle state machine.
 *
 * CONFIRMED --> CANCELLED (by user, within cancellation window)
 * CONFIRMED --> COMPLETED (after the slot's end time)
 * CANCELLED / COMPLETED are terminal states.
 */
public enum BookingStatus {
    CONFIRMED,
    CANCELLED,
    COMPLETED;

    private Set<BookingStatus> allowedTransitions() {
        return switch (this) {
            case CONFIRMED -> EnumSet.of(CANCELLED, COMPLETED);
            case CANCELLED, COMPLETED -> EnumSet.noneOf(BookingStatus.class);
        };
    }

    public boolean canTransitionTo(BookingStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return this == CANCELLED || this == COMPLETED;
    }
}

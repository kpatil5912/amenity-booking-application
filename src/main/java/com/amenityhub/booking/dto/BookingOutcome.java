package com.amenityhub.booking.dto;

/**
 * Result of a booking attempt: either a confirmed booking, or a waitlist
 * placement when the slot was full and the caller opted to wait.
 */
public sealed interface BookingOutcome permits BookingOutcome.Confirmed, BookingOutcome.Waitlisted {

    record Confirmed(BookingResponse booking) implements BookingOutcome {
    }

    record Waitlisted(WaitlistResponse waitlist) implements BookingOutcome {
    }
}

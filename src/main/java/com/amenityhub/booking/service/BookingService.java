package com.amenityhub.booking.service;
import com.amenityhub.booking.entity.Booking;
import com.amenityhub.booking.entity.BookingStatus;
import com.amenityhub.booking.repository.BookingRepository;

import com.amenityhub.auth.service.CurrentUserService;
import com.amenityhub.booking.dto.BookingOutcome;
import com.amenityhub.booking.dto.BookingResponse;
import com.amenityhub.booking.dto.CreateBookingRequest;
import com.amenityhub.booking.dto.WaitlistResponse;
import com.amenityhub.common.exception.BusinessRuleException;
import com.amenityhub.common.exception.ConflictException;
import com.amenityhub.common.exception.NotFoundException;
import com.amenityhub.notification.service.BookingEmailSender;
import com.amenityhub.resource.entity.AvailabilitySlot;
import com.amenityhub.resource.repository.AvailabilitySlotRepository;
import com.amenityhub.user.entity.User;
import com.amenityhub.waitlist.entity.WaitlistEntry;
import com.amenityhub.waitlist.repository.WaitlistEntryRepository;
import com.amenityhub.waitlist.entity.WaitlistStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final WaitlistEntryRepository waitlistRepository;
    private final BookingReferenceGenerator referenceGenerator;
    private final CurrentUserService currentUserService;
    private final BookingEmailSender emailSender;

    public BookingService(
            BookingRepository bookingRepository,
            AvailabilitySlotRepository slotRepository,
            WaitlistEntryRepository waitlistRepository,
            BookingReferenceGenerator referenceGenerator,
            CurrentUserService currentUserService,
            BookingEmailSender emailSender) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.waitlistRepository = waitlistRepository;
        this.referenceGenerator = referenceGenerator;
        this.currentUserService = currentUserService;
        this.emailSender = emailSender;
    }

    /**
     * Books a slot for the current user.
     *
     * Concurrency: the slot row is loaded with a pessimistic write lock
     * (SELECT ... FOR UPDATE), so concurrent booking attempts on the same slot
     * are serialized and capacity cannot be over-committed.
     */
    @Transactional
    public BookingOutcome book(CreateBookingRequest request) {
        User user = currentUserService.requireCurrentUser();

        // Lock the slot for the duration of the transaction.
        AvailabilitySlot slot = slotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new NotFoundException("Slot not found: " + request.slotId()));

        if (slot.getEndTime().isBefore(Instant.now())) {
            throw new BusinessRuleException("Cannot book a slot that is already in the past");
        }

        if (bookingRepository.existsBySlotIdAndUserIdAndStatus(
                slot.getId(), user.getId(), BookingStatus.CONFIRMED)) {
            throw new ConflictException("You already have a confirmed booking for this slot");
        }

        if (!slot.hasFreeCapacity()) {
            if (request.joinWaitlistIfFull()) {
                return new BookingOutcome.Waitlisted(joinWaitlist(slot, user));
            }
            throw new BusinessRuleException("Slot is fully booked");
        }

        slot.setBookedCount(slot.getBookedCount() + 1);
        slotRepository.save(slot);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingReference(referenceGenerator.generate());

        Booking saved = persistWithUniqueReference(booking);
        emailSender.sendBookingConfirmed(saved);
        return new BookingOutcome.Confirmed(BookingResponse.from(saved));
    }

    @Transactional
    public BookingResponse cancel(Long bookingId) {
        User user = currentUserService.requireCurrentUser();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Booking not found: " + bookingId);
        }

        if (booking.getStatus().isTerminal()) {
            throw new BusinessRuleException(
                    "Booking is already " + booking.getStatus() + " and cannot be cancelled");
        }

        // Lock the slot so capacity release + waitlist promotion is atomic.
        AvailabilitySlot slot = slotRepository.findByIdForUpdate(booking.getSlot().getId())
                .orElseThrow(() -> new NotFoundException("Slot not found"));

        enforceCancellationWindow(slot);

        booking.transitionTo(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Release the freed seat, then try to promote the next waitlisted user.
        slot.setBookedCount(Math.max(0, slot.getBookedCount() - 1));
        slotRepository.save(slot);

        emailSender.sendBookingCancelled(booking);

        promoteFromWaitlist(slot);

        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings() {
        User user = currentUserService.requireCurrentUser();
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getByReference(String reference) {
        User user = currentUserService.requireCurrentUser();
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + reference));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Booking not found: " + reference);
        }
        return BookingResponse.from(booking);
    }

    // ---- internal helpers -------------------------------------------------

    private void enforceCancellationWindow(AvailabilitySlot slot) {
        int windowHours = slot.getResource().getCancellationWindowHours();
        Instant cutoff = slot.getStartTime().minus(windowHours, ChronoUnit.HOURS);
        if (Instant.now().isAfter(cutoff)) {
            throw new BusinessRuleException(
                    "Cancellation window has passed (must cancel at least "
                            + windowHours + "h before start)");
        }
    }

    private WaitlistResponse joinWaitlist(AvailabilitySlot slot, User user) {
        if (waitlistRepository.existsBySlotIdAndUserIdAndStatus(
                slot.getId(), user.getId(), WaitlistStatus.WAITING)) {
            throw new ConflictException("You are already on the waitlist for this slot");
        }

        int position = waitlistRepository.countBySlotIdAndStatus(
                slot.getId(), WaitlistStatus.WAITING) + 1;

        WaitlistEntry entry = new WaitlistEntry();
        entry.setSlot(slot);
        entry.setUser(user);
        entry.setStatus(WaitlistStatus.WAITING);
        entry.setPosition(position);
        waitlistRepository.save(entry);

        return new WaitlistResponse(
                entry.getId(), slot.getId(), position,
                "Slot is full. You have been added to the waitlist at position " + position);
    }

    /**
     * Promotes the next waiting user into a freed seat, if any. Runs inside the
     * same transaction as the cancellation, on the already-locked slot.
     */
    private void promoteFromWaitlist(AvailabilitySlot slot) {
        if (!slot.hasFreeCapacity()) {
            return;
        }

        Optional<WaitlistEntry> next = waitlistRepository
                .findFirstBySlotIdAndStatusOrderByPositionAsc(slot.getId(), WaitlistStatus.WAITING);
        if (next.isEmpty()) {
            return;
        }

        WaitlistEntry entry = next.get();
        entry.setStatus(WaitlistStatus.PROMOTED);
        waitlistRepository.save(entry);

        slot.setBookedCount(slot.getBookedCount() + 1);
        slotRepository.save(slot);

        Booking booking = new Booking();
        booking.setUser(entry.getUser());
        booking.setSlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingReference(referenceGenerator.generate());

        Booking saved = persistWithUniqueReference(booking);
        emailSender.sendWaitlistPromoted(saved);
    }

    /**
     * Saves the booking, retrying with a fresh reference on the rare chance of a
     * booking_reference collision.
     */
    private Booking persistWithUniqueReference(Booking booking) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return bookingRepository.saveAndFlush(booking);
            } catch (DataIntegrityViolationException ex) {
                booking.setBookingReference(referenceGenerator.generate());
            }
        }
        throw new ConflictException("Could not generate a unique booking reference");
    }
}

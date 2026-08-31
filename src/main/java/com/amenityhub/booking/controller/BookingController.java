package com.amenityhub.booking.controller;
import com.amenityhub.booking.service.BookingService;

import com.amenityhub.booking.dto.BookingOutcome;
import com.amenityhub.booking.dto.BookingResponse;
import com.amenityhub.booking.dto.CreateBookingRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingOutcome> book(@Valid @RequestBody CreateBookingRequest request) {
        BookingOutcome outcome = bookingService.book(request);
        HttpStatus status = outcome instanceof BookingOutcome.Confirmed
                ? HttpStatus.CREATED
                : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(outcome);
    }

    @GetMapping
    public List<BookingResponse> myBookings() {
        return bookingService.myBookings();
    }

    @GetMapping("/{reference}")
    public BookingResponse getByReference(@PathVariable String reference) {
        return bookingService.getByReference(reference);
    }

    @DeleteMapping("/{bookingId}")
    public BookingResponse cancel(@PathVariable Long bookingId) {
        return bookingService.cancel(bookingId);
    }
}

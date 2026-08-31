package com.amenityhub.booking.repository;
import com.amenityhub.booking.entity.Booking;
import com.amenityhub.booking.entity.BookingStatus;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsBySlotIdAndUserIdAndStatus(Long slotId, Long userId, BookingStatus status);
}

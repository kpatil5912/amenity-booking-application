package com.amenityhub.resource.repository;
import com.amenityhub.resource.entity.AvailabilitySlot;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    List<AvailabilitySlot> findByResourceIdAndStartTimeBetweenOrderByStartTime(
            Long resourceId, Instant from, Instant to);

    /**
     * Loads a slot with a pessimistic write lock (SELECT ... FOR UPDATE).
     * Serializes concurrent booking attempts on the same slot so two callers
     * cannot both read stale capacity and over-book.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AvailabilitySlot s WHERE s.id = :id")
    Optional<AvailabilitySlot> findByIdForUpdate(@Param("id") Long id);
}

package com.amenityhub.waitlist.repository;
import com.amenityhub.waitlist.entity.WaitlistEntry;
import com.amenityhub.waitlist.entity.WaitlistStatus;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findBySlotIdAndStatusOrderByPositionAsc(Long slotId, WaitlistStatus status);

    Optional<WaitlistEntry> findFirstBySlotIdAndStatusOrderByPositionAsc(
            Long slotId, WaitlistStatus status);

    int countBySlotIdAndStatus(Long slotId, WaitlistStatus status);

    boolean existsBySlotIdAndUserIdAndStatus(Long slotId, Long userId, WaitlistStatus status);
}

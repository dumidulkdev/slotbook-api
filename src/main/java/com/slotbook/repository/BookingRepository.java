package com.slotbook.repository;

import com.slotbook.domain.BookingEntity;
import com.slotbook.domain.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    @Query("""
            SELECT b FROM BookingEntity b
            WHERE b.client.id = :userId OR b.provider.id = :userId
            ORDER BY b.startTime ASC
            """)
    Page<BookingEntity> findMine(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT b FROM BookingEntity b
            WHERE b.provider.id = :providerId
              AND b.status = :status
              AND b.startTime >= :dayStart
              AND b.startTime < :dayEnd
            """)
    List<BookingEntity> findActiveForProviderOnDay(
            @Param("providerId") Long providerId,
            @Param("status") BookingStatus status,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    Optional<BookingEntity> findByProviderIdAndStartTimeAndStatus(
            Long providerId, LocalDateTime startTime, BookingStatus status);
}
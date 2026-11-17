package com.slotbook.service;

import com.slotbook.config.AppProperties;
import com.slotbook.domain.BookingEntity;
import com.slotbook.domain.BookingStatus;
import com.slotbook.exception.ApiException;
import com.slotbook.exception.ConflictException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class BookingRules {

    private final AppProperties appProperties;
    private final Clock clock;
    private final ZoneId zoneId;

    public BookingRules(AppProperties appProperties, Clock clock) {
        this.appProperties = appProperties;
        this.clock = clock;
        this.zoneId = appProperties.zoneId();
    }

    public void assertCancellable(BookingEntity booking) {
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new ConflictException("Only booked appointments can be cancelled");
        }
        LocalDateTime now = LocalDateTime.now(clock.withZone(zoneId));
        Duration untilStart = Duration.between(now, booking.getStartTime());
        long cutoffHours = appProperties.getBooking().getCancellationCutoffHours();
        if (untilStart.toHours() < cutoffHours) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cancellation is not allowed within " + cutoffHours + " hours of the appointment");
        }
    }

    public void assertProviderOutcomeAllowed(BookingEntity booking) {
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new ConflictException("Invalid status transition from " + booking.getStatus());
        }
        LocalDateTime now = LocalDateTime.now(clock.withZone(zoneId));
        if (booking.getStartTime().isAfter(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Outcome can only be recorded after the appointment start time");
        }
    }

    public void transition(BookingEntity booking, BookingStatus target) {
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new ConflictException("Invalid status transition from " + booking.getStatus());
        }
        if (target != BookingStatus.CANCELLED
                && target != BookingStatus.COMPLETED
                && target != BookingStatus.NO_SHOW) {
            throw new ConflictException("Unsupported target status " + target);
        }
        booking.setStatus(target);
    }
}
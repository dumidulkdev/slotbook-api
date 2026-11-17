package com.slotbook.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.slotbook.config.AppProperties;
import com.slotbook.domain.BookingEntity;
import com.slotbook.domain.BookingStatus;
import com.slotbook.domain.Role;
import com.slotbook.domain.UserEntity;
import com.slotbook.exception.ApiException;
import com.slotbook.exception.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingRulesTest {

    private BookingRules bookingRules;
    private UserEntity provider;
    private UserEntity client;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getBooking().setCancellationCutoffHours(2);
        properties.setTimezone("UTC");
        Clock clock = Clock.fixed(Instant.parse("2026-06-16T10:00:00Z"), ZoneId.of("UTC"));
        bookingRules = new BookingRules(properties, clock);
        provider = new UserEntity("P", "p@test.dev", "h", Role.PROVIDER);
        client = new UserEntity("C", "c@test.dev", "h", Role.CLIENT);
    }

    @Test
    void rejectsCancellationInsideCutoffWindow() {
        BookingEntity booking = new BookingEntity(provider, client,
                LocalDateTime.of(2026, 6, 16, 11, 0), LocalDateTime.of(2026, 6, 16, 11, 30));

        assertThatThrownBy(() -> bookingRules.assertCancellable(booking))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void allowsCancellationOutsideCutoffWindow() {
        BookingEntity booking = new BookingEntity(provider, client,
                LocalDateTime.of(2026, 6, 16, 14, 0), LocalDateTime.of(2026, 6, 16, 14, 30));

        bookingRules.assertCancellable(booking);
        bookingRules.transition(booking, BookingStatus.CANCELLED);
    }

    @Test
    void rejectsInvalidStatusTransitions() {
        BookingEntity booking = new BookingEntity(provider, client,
                LocalDateTime.of(2026, 6, 16, 9, 0), LocalDateTime.of(2026, 6, 16, 9, 30));
        booking.setStatus(BookingStatus.CANCELLED);

        assertThatThrownBy(() -> bookingRules.transition(booking, BookingStatus.COMPLETED))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void providerOutcomeRequiresStartTimePassed() {
        BookingEntity future = new BookingEntity(provider, client,
                LocalDateTime.of(2026, 6, 16, 12, 0), LocalDateTime.of(2026, 6, 16, 12, 30));

        assertThatThrownBy(() -> bookingRules.assertProviderOutcomeAllowed(future))
                .isInstanceOf(ApiException.class);
    }
}
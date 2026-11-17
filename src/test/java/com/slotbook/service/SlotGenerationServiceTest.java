package com.slotbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.slotbook.config.AppProperties;
import com.slotbook.domain.BookingEntity;
import com.slotbook.domain.BookingStatus;
import com.slotbook.domain.ProviderAvailabilityEntity;
import com.slotbook.domain.UserEntity;
import com.slotbook.domain.WorkingWindowEmbeddable;
import com.slotbook.domain.Role;
import com.slotbook.dto.slot.SlotResponse;
import com.slotbook.repository.BookingRepository;
import com.slotbook.repository.ProviderAvailabilityRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlotGenerationServiceTest {

    @Mock
    private ProviderAvailabilityRepository availabilityRepository;
    @Mock
    private BookingRepository bookingRepository;

    private SlotGenerationService slotGenerationService;
    private ProviderAvailabilityEntity availability;
    private UserEntity provider;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setTimezone("UTC");
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-16T08:00:00Z"), ZoneId.of("UTC"));
        slotGenerationService = new SlotGenerationService(
                availabilityRepository, bookingRepository, fixedClock, properties);
        provider = new UserEntity("Provider", "p@test.dev", "hash", Role.PROVIDER);
        availability = new ProviderAvailabilityEntity(provider, 30, List.of(
                new WorkingWindowEmbeddable(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0))));
    }

    @Test
    void generatesSlotsAlignedToSlotLength() {
        when(availabilityRepository.findByProviderId(1L)).thenReturn(Optional.of(availability));
        when(bookingRepository.findActiveForProviderOnDay(eq(1L), eq(BookingStatus.BOOKED), any(), any()))
                .thenReturn(List.of());

        LocalDate date = LocalDate.of(2026, 6, 16);
        List<SlotResponse> slots = slotGenerationService.generateFreeSlots(1L, date);

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).start()).isEqualTo(LocalDateTime.of(2026, 6, 16, 9, 0));
        assertThat(slots.get(1).start()).isEqualTo(LocalDateTime.of(2026, 6, 16, 9, 30));
    }

    @Test
    void filtersBookedAndPastSlots() {
        when(availabilityRepository.findByProviderId(1L)).thenReturn(Optional.of(availability));
        LocalDate date = LocalDate.of(2026, 6, 16);
        BookingEntity booked = new BookingEntity(provider, provider,
                LocalDateTime.of(2026, 6, 16, 9, 30), LocalDateTime.of(2026, 6, 16, 10, 0));
        when(bookingRepository.findActiveForProviderOnDay(eq(1L), eq(BookingStatus.BOOKED), any(), any()))
                .thenReturn(List.of(booked));

        List<SlotResponse> slots = slotGenerationService.generateFreeSlots(1L, date);

        assertThat(slots).hasSize(1);
        assertThat(slots.get(0).start()).isEqualTo(LocalDateTime.of(2026, 6, 16, 9, 0));
    }

    @Test
    void respectsOddSlotLength() {
        when(availabilityRepository.findByProviderId(1L)).thenReturn(Optional.of(availability));
        availability = new ProviderAvailabilityEntity(provider, 25, List.of(
                new WorkingWindowEmbeddable(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(availabilityRepository.findByProviderId(1L)).thenReturn(Optional.of(availability));
        when(bookingRepository.findActiveForProviderOnDay(eq(1L), eq(BookingStatus.BOOKED), any(), any()))
                .thenReturn(List.of());

        List<SlotResponse> slots = slotGenerationService.generateFreeSlots(1L, LocalDate.of(2026, 6, 16));

        assertThat(slots).extracting(SlotResponse::start)
                .containsExactly(
                        LocalDateTime.of(2026, 6, 16, 9, 0),
                        LocalDateTime.of(2026, 6, 16, 9, 25));
    }

    @Test
    void matchesAvailabilityRequiresGridAlignment() {
        AppProperties properties = new AppProperties();
        properties.setTimezone("UTC");
        SlotGenerationService service = new SlotGenerationService(
                availabilityRepository, bookingRepository, Clock.systemUTC(), properties);

        boolean valid = service.matchesAvailability(availability, LocalDateTime.of(2026, 6, 16, 9, 0));
        boolean invalid = service.matchesAvailability(availability, LocalDateTime.of(2026, 6, 16, 9, 15));

        assertThat(valid).isTrue();
        assertThat(invalid).isFalse();
    }
}
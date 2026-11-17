package com.slotbook.service;

import com.slotbook.domain.BookingEntity;
import com.slotbook.domain.BookingStatus;
import com.slotbook.domain.ProviderAvailabilityEntity;
import com.slotbook.domain.WorkingWindowEmbeddable;
import com.slotbook.dto.slot.SlotResponse;
import com.slotbook.exception.ResourceNotFoundException;
import com.slotbook.repository.BookingRepository;
import com.slotbook.repository.ProviderAvailabilityRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SlotGenerationService {

    private final ProviderAvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;
    private final ZoneId zoneId;

    public SlotGenerationService(ProviderAvailabilityRepository availabilityRepository,
                                 BookingRepository bookingRepository,
                                 Clock clock,
                                 com.slotbook.config.AppProperties appProperties) {
        this.availabilityRepository = availabilityRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
        this.zoneId = appProperties.zoneId();
    }

    public List<SlotResponse> generateFreeSlots(Long providerId, LocalDate date) {
        ProviderAvailabilityEntity availability = availabilityRepository.findByProviderId(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider availability not configured"));

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        List<BookingEntity> booked = bookingRepository.findActiveForProviderOnDay(
                providerId, BookingStatus.BOOKED, dayStart, dayEnd);
        Set<LocalDateTime> bookedStarts = new HashSet<>();
        for (BookingEntity booking : booked) {
            bookedStarts.add(booking.getStartTime());
        }

        List<SlotResponse> slots = new ArrayList<>();
        int slotLength = availability.getSlotLengthMinutes();
        LocalDateTime now = LocalDateTime.now(clock.withZone(zoneId));

        for (WorkingWindowEmbeddable window : availability.getWorkingWindows()) {
            if (!window.getDayOfWeek().equals(date.getDayOfWeek())) {
                continue;
            }
            LocalDateTime slotStart = date.atTime(window.getStartTime());
            LocalDateTime windowEnd = date.atTime(window.getEndTime());
            while (!slotStart.plusMinutes(slotLength).isAfter(windowEnd)) {
                LocalDateTime slotEnd = slotStart.plusMinutes(slotLength);
                if (!slotStart.isBefore(now) && !bookedStarts.contains(slotStart)) {
                    slots.add(new SlotResponse(slotStart, slotEnd));
                }
                slotStart = slotStart.plusMinutes(slotLength);
            }
        }
        slots.sort((a, b) -> a.start().compareTo(b.start()));
        return slots;
    }

    public boolean matchesAvailability(ProviderAvailabilityEntity availability,
                                         LocalDateTime slotStart) {
        int slotLength = availability.getSlotLengthMinutes();
        LocalDate date = slotStart.toLocalDate();
        LocalDateTime slotEnd = slotStart.plusMinutes(slotLength);

        for (WorkingWindowEmbeddable window : availability.getWorkingWindows()) {
            if (!window.getDayOfWeek().equals(date.getDayOfWeek())) {
                continue;
            }
            LocalDateTime windowStart = date.atTime(window.getStartTime());
            LocalDateTime windowEnd = date.atTime(window.getEndTime());
            boolean startsOnBoundary = fitsSlotGrid(windowStart, slotStart, slotLength);
            boolean withinWindow = !slotStart.isBefore(windowStart)
                    && !slotEnd.isAfter(windowEnd);
            if (startsOnBoundary && withinWindow) {
                return true;
            }
        }
        return false;
    }

    private boolean fitsSlotGrid(LocalDateTime windowStart, LocalDateTime slotStart, int slotLength) {
        long minutesFromStart = java.time.Duration.between(windowStart, slotStart).toMinutes();
        return minutesFromStart >= 0 && minutesFromStart % slotLength == 0;
    }
}
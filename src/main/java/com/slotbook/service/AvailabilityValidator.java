package com.slotbook.service;

import com.slotbook.dto.availability.WorkingWindowRequest;
import com.slotbook.exception.ApiException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityValidator {

    public void validate(int slotLengthMinutes, List<WorkingWindowRequest> windows) {
        if (slotLengthMinutes < 5 || slotLengthMinutes > 24 * 60) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Slot length must be between 5 and 1440 minutes");
        }
        Map<DayOfWeek, List<WorkingWindowRequest>> byDay = new EnumMap<>(DayOfWeek.class);
        for (WorkingWindowRequest window : windows) {
            if (!window.end().isAfter(window.start())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "End time must be after start time for " + window.dayOfWeek());
            }
            long minutes = java.time.Duration.between(window.start(), window.end()).toMinutes();
            if (minutes < slotLengthMinutes) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Working window must be at least as long as slot length on " + window.dayOfWeek());
            }
            byDay.computeIfAbsent(window.dayOfWeek(), d -> new ArrayList<>()).add(window);
        }
        for (Map.Entry<DayOfWeek, List<WorkingWindowRequest>> entry : byDay.entrySet()) {
            List<WorkingWindowRequest> dayWindows = new ArrayList<>(entry.getValue());
            dayWindows.sort(Comparator.comparing(WorkingWindowRequest::start));
            for (int i = 1; i < dayWindows.size(); i++) {
                WorkingWindowRequest prev = dayWindows.get(i - 1);
                WorkingWindowRequest curr = dayWindows.get(i);
                if (curr.start().isBefore(prev.end())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Overlapping working windows on " + entry.getKey());
                }
            }
        }
    }
}
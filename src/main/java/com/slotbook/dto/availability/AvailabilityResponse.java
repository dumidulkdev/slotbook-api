package com.slotbook.dto.availability;

import java.util.List;

public record AvailabilityResponse(
        Long providerId,
        int slotLengthMinutes,
        List<WorkingWindowRequest> workingWindows
) {
}
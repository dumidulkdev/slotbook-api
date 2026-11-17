package com.slotbook.dto.availability;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AvailabilityRequest(
        @NotNull @Min(5) Integer slotLengthMinutes,
        @NotEmpty @Valid List<WorkingWindowRequest> workingWindows
) {
}
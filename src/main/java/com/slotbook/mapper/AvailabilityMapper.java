package com.slotbook.mapper;

import com.slotbook.domain.ProviderAvailabilityEntity;
import com.slotbook.domain.WorkingWindowEmbeddable;
import com.slotbook.dto.availability.AvailabilityResponse;
import com.slotbook.dto.availability.WorkingWindowRequest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityMapper {

    public AvailabilityResponse toResponse(ProviderAvailabilityEntity entity) {
        List<WorkingWindowRequest> windows = entity.getWorkingWindows().stream()
                .map(w -> new WorkingWindowRequest(w.getDayOfWeek(), w.getStartTime(), w.getEndTime()))
                .toList();
        return new AvailabilityResponse(entity.getProvider().getId(), entity.getSlotLengthMinutes(), windows);
    }

    public List<WorkingWindowEmbeddable> toEmbeddables(List<WorkingWindowRequest> windows) {
        return windows.stream()
                .map(w -> new WorkingWindowEmbeddable(w.dayOfWeek(), w.start(), w.end()))
                .toList();
    }
}
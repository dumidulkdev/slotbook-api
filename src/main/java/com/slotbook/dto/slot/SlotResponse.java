package com.slotbook.dto.slot;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record SlotResponse(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime start,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime end
) {
}
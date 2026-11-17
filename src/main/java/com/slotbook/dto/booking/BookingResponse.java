package com.slotbook.dto.booking;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.slotbook.domain.BookingStatus;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long providerId,
        String providerName,
        Long clientId,
        String clientName,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime,
        BookingStatus status
) {
}
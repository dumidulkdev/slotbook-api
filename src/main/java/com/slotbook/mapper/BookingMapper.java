package com.slotbook.mapper;

import com.slotbook.domain.BookingEntity;
import com.slotbook.dto.booking.BookingResponse;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(BookingEntity booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getProvider().getId(),
                booking.getProvider().getName(),
                booking.getClient().getId(),
                booking.getClient().getName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus());
    }
}
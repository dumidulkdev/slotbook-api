package com.slotbook.controller;

import com.slotbook.dto.ApiResponse;
import com.slotbook.dto.booking.BookingResponse;
import com.slotbook.dto.booking.CreateBookingRequest;
import com.slotbook.security.SecurityUtils;
import com.slotbook.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ApiResponse<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        return ApiResponse.ok(bookingService.createBooking(SecurityUtils.currentUser(), request));
    }

    @GetMapping("/me")
    public ApiResponse<Page<BookingResponse>> myBookings(
            @PageableDefault(sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.ok(bookingService.myBookings(SecurityUtils.currentUser(), pageable));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<BookingResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.cancel(SecurityUtils.currentUser(), id));
    }

    @PatchMapping("/{id}/complete")
    public ApiResponse<BookingResponse> complete(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.complete(SecurityUtils.currentUser(), id));
    }

    @PatchMapping("/{id}/no-show")
    public ApiResponse<BookingResponse> noShow(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.noShow(SecurityUtils.currentUser(), id));
    }
}
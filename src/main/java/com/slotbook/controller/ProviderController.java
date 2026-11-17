package com.slotbook.controller;

import com.slotbook.dto.ApiResponse;
import com.slotbook.dto.availability.AvailabilityRequest;
import com.slotbook.dto.availability.AvailabilityResponse;
import com.slotbook.dto.provider.ProviderSummaryResponse;
import com.slotbook.dto.slot.SlotResponse;
import com.slotbook.security.SecurityUtils;
import com.slotbook.service.AvailabilityService;
import com.slotbook.service.ProviderService;
import com.slotbook.service.SlotGenerationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ProviderService providerService;
    private final AvailabilityService availabilityService;
    private final SlotGenerationService slotGenerationService;

    public ProviderController(ProviderService providerService,
                              AvailabilityService availabilityService,
                              SlotGenerationService slotGenerationService) {
        this.providerService = providerService;
        this.availabilityService = availabilityService;
        this.slotGenerationService = slotGenerationService;
    }

    @GetMapping
    public ApiResponse<Page<ProviderSummaryResponse>> listProviders(Pageable pageable) {
        return ApiResponse.ok(providerService.listProviders(pageable));
    }

    @PostMapping("/me/availability")
    public ApiResponse<AvailabilityResponse> createAvailability(@Valid @RequestBody AvailabilityRequest request) {
        return ApiResponse.ok(availabilityService.setAvailability(SecurityUtils.currentUser(), request));
    }

    @PutMapping("/me/availability")
    public ApiResponse<AvailabilityResponse> updateAvailability(@Valid @RequestBody AvailabilityRequest request) {
        return ApiResponse.ok(availabilityService.setAvailability(SecurityUtils.currentUser(), request));
    }

    @GetMapping("/me/availability")
    public ApiResponse<AvailabilityResponse> myAvailability() {
        return ApiResponse.ok(availabilityService.getMyAvailability(SecurityUtils.currentUser()));
    }

    @GetMapping("/{id}/slots")
    public ApiResponse<List<SlotResponse>> freeSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(slotGenerationService.generateFreeSlots(id, date));
    }
}
package com.slotbook.dto.provider;

public record ProviderSummaryResponse(
        Long id,
        String name,
        String email
) {
}
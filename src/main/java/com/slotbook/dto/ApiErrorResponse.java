package com.slotbook.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        boolean success,
        String message,
        int status,
        String path,
        Instant timestamp,
        List<FieldErrorDetail> errors
) {
    public record FieldErrorDetail(String field, String message) {
    }

    public static ApiErrorResponse of(int status, String path, String message) {
        return new ApiErrorResponse(false, message, status, path, Instant.now(), null);
    }

    public static ApiErrorResponse of(int status, String path, String message, List<FieldErrorDetail> errors) {
        return new ApiErrorResponse(false, message, status, path, Instant.now(), errors);
    }
}
package com.codewithsubhra.money_tracker_backend.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Uniform envelope returned by every endpoint so the client always parses the
 * same shape regardless of success or failure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> fail(ApiError error) {
        return new ApiResponse<>(false, null, error, Instant.now());
    }

    /** Machine-readable error detail. */
    public record ApiError(String code, String message, Object details) {
    }
}

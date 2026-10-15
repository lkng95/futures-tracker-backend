package com.lk.futurestracker.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Uniform error body returned for every failure case, so API consumers only need to handle one shape. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> fieldErrors
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(LocalDateTime.now(), status, error, message, null);
    }

    public static ApiError of(int status, String error, String message, List<String> fieldErrors) {
        return new ApiError(LocalDateTime.now(), status, error, message, fieldErrors);
    }
}

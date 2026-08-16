package io.github.carloscardoso05.api.shared;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ApiError(
        @NotNull Instant timestamp,
        @NotNull int status,
        @NotNull String error,
        @NotNull String message,
        @NotNull String path) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}

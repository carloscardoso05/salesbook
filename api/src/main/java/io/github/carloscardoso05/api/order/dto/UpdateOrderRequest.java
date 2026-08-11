package io.github.carloscardoso05.api.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Request payload for updating an existing order. Only non-null fields are applied.")
public record UpdateOrderRequest(
        @Schema(description = "New order date and time")
        Instant orderedAt
) {
}

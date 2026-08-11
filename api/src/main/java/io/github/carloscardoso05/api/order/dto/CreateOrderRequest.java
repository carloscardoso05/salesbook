package io.github.carloscardoso05.api.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "Request payload for creating a new order")
public record CreateOrderRequest(
        @NotNull
        @Schema(description = "ID of the customer placing the order", example = "1")
        Integer customerId,

        @Schema(description = "Order date and time. Defaults to now if not provided")
        Instant orderedAt
) {
    @Override
    public Instant orderedAt() {
        return orderedAt != null ? orderedAt : Instant.now();
    }
}

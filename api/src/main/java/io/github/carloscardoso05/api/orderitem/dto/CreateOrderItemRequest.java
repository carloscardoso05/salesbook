package io.github.carloscardoso05.api.orderitem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(description = "Request payload for creating a new order item")
public record CreateOrderItemRequest(
        @NotNull
        @Schema(description = "ID of the product being ordered", example = "1")
        Integer productId,

        @NotNull
        @PositiveOrZero
        @Schema(description = "Item price", example = "25.00")
        BigDecimal price
) {
}

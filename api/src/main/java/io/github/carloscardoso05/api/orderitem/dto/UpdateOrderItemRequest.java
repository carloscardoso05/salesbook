package io.github.carloscardoso05.api.orderitem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(description = "Request payload for updating an existing order item. Only non-null fields are applied.")
public record UpdateOrderItemRequest(
        @PositiveOrZero
        @Schema(description = "New item price", example = "30.00")
        BigDecimal price
) {
}

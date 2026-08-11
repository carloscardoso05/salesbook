package io.github.carloscardoso05.api.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Request payload for updating an existing product. Only non-null fields are applied.")
public record UpdateProductRequest(
        @Schema(description = "New product name", example = "Coffee")
        String name,

        @PositiveOrZero
        @Schema(description = "New product stock", example = "15")
        Integer stock
) {
}

package io.github.carloscardoso05.api.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Request payload for creating a new product")
public record CreateProductRequest(
        @NotBlank
        @Schema(description = "Product name", example = "Coffee")
        String name,

        @PositiveOrZero
        @Schema(description = "Product stock. Defaults to 0 if not provided", example = "10", defaultValue = "0")
        Integer stock
) {
    @Override
    public Integer stock() {
        return stock != null ? stock : 0;
    }
}

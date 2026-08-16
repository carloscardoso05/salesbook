package io.github.carloscardoso05.api.product.dto;

import io.github.carloscardoso05.api.product.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Product response DTO")
public record ProductDto(
        @Schema(description = "Product ID", example = "1")
        @NotNull
        Integer id,

        @Schema(description = "Product name", example = "Coffee")
        @NotNull
        String name,

        @Schema(description = "Product stock", example = "10")
        @NotNull
        Integer stock
) {
    public static ProductDto of(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getStock()
        );
    }
}

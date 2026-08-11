package io.github.carloscardoso05.api.product.dto;

import io.github.carloscardoso05.api.product.Product;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Product response DTO")
public record ProductDto(
        @Schema(description = "Product ID", example = "1")
        Integer id,

        @Schema(description = "Product name", example = "Coffee")
        String name,

        @Schema(description = "Product stock", example = "10")
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

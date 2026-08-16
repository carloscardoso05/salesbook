package io.github.carloscardoso05.api.orderitem.dto;

import io.github.carloscardoso05.api.orderitem.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Order item response DTO")
public record OrderItemDto(
        @Schema(description = "Order item ID", example = "1")
        @NotNull
        Integer id,

        @Schema(description = "Order ID", example = "1")
        @NotNull
        Integer orderId,

        @Schema(description = "Product ID", example = "1")
        @NotNull
        Integer productId,

        @Schema(description = "Product name", example = "Coffee")
        @NotNull
        String productName,

        @Schema(description = "Item price", example = "25.00")
        @NotNull
        BigDecimal price
) {
    public static OrderItemDto of(OrderItem orderItem) {
        return new OrderItemDto(
                orderItem.getId(),
                orderItem.getOrder().getId(),
                orderItem.getProduct().getId(),
                orderItem.getProduct().getName(),
                orderItem.getPrice()
        );
    }
}

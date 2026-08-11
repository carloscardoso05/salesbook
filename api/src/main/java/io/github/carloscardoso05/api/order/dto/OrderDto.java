package io.github.carloscardoso05.api.order.dto;

import io.github.carloscardoso05.api.order.Order;
import io.github.carloscardoso05.api.orderitem.OrderItem;
import io.github.carloscardoso05.api.orderitem.dto.OrderItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Order response DTO")
public record OrderDto(
        @Schema(description = "Order ID", example = "1")
        Integer id,

        @Schema(description = "Customer ID", example = "1")
        Integer customerId,

        @Schema(description = "Customer name", example = "Carlos")
        String customerName,

        @Schema(description = "Order date and time")
        Instant orderedAt,

        @Schema(description = "Total price of all order items", example = "150.00")
        BigDecimal totalPrice,

        @Schema(description = "Order items")
        List<OrderItemDto> items
) {
    public static OrderDto of(Order order, List<OrderItem> items) {
        var totalPrice = items.stream()
                .map(OrderItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var itemDtos = items.stream()
                .map(OrderItemDto::of)
                .toList();
        return new OrderDto(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getOrderedAt(),
                totalPrice,
                itemDtos
        );
    }
}

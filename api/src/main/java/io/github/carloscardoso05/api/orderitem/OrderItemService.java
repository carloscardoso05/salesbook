package io.github.carloscardoso05.api.orderitem;

import io.github.carloscardoso05.api.order.Order;
import io.github.carloscardoso05.api.order.OrderRepository;
import io.github.carloscardoso05.api.orderitem.dto.CreateOrderItemRequest;
import io.github.carloscardoso05.api.orderitem.dto.OrderItemDto;
import io.github.carloscardoso05.api.orderitem.dto.UpdateOrderItemRequest;
import io.github.carloscardoso05.api.product.Product;
import io.github.carloscardoso05.api.product.ProductRepository;
import io.github.carloscardoso05.api.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<OrderItemDto> listItems(Integer orderId) {
        getOrderById(orderId);
        return orderItemRepository.findAllByOrderId(orderId).stream()
                .map(OrderItemDto::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderItemDto findItem(Integer orderId, Integer itemId) {
        return OrderItemDto.of(getOrderItemInOrder(orderId, itemId));
    }

    @Transactional
    public OrderItemDto createItem(Integer orderId, CreateOrderItemRequest request) {
        var order = getOrderById(orderId);
        var product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException(Product.class, request.productId()));
        if (product.getStock() < 1) {
            throw new IllegalArgumentException("Product '%s' has insufficient stock.".formatted(product.getName()));
        }
        order.getCustomer().addToBalance(request.price().negate());
        product.addToStock(-1);
        var orderItem = new OrderItem(order, product, request.price());
        return OrderItemDto.of(orderItemRepository.save(orderItem));
    }

    @Transactional
    public OrderItemDto updateItem(Integer orderId, Integer itemId, UpdateOrderItemRequest request) {
        var orderItem = getOrderItemInOrder(orderId, itemId);
        if (request.price() != null) {
            var difference = orderItem.getPrice().subtract(request.price());
            orderItem.getOrder().getCustomer().addToBalance(difference);
            orderItem.setPrice(request.price());
        }
        return OrderItemDto.of(orderItem);
    }

    @Transactional
    public void deleteItem(Integer orderId, Integer itemId) {
        var orderItem = getOrderItemInOrder(orderId, itemId);
        orderItem.getOrder().getCustomer().addToBalance(orderItem.getPrice());
        orderItem.getProduct().addToStock(1);
        orderItemRepository.delete(orderItem);
    }

    private Order getOrderById(Integer orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException(Order.class, orderId));
    }

    private OrderItem getOrderItemInOrder(Integer orderId, Integer itemId) {
        return orderItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new NotFoundException(OrderItem.class, itemId));
    }
}

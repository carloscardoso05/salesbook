package io.github.carloscardoso05.api.order;

import io.github.carloscardoso05.api.customer.Customer;
import io.github.carloscardoso05.api.customer.CustomerRepository;
import io.github.carloscardoso05.api.order.dto.CreateOrderRequest;
import io.github.carloscardoso05.api.order.dto.OrderDto;
import io.github.carloscardoso05.api.order.dto.UpdateOrderRequest;
import io.github.carloscardoso05.api.orderitem.OrderItem;
import io.github.carloscardoso05.api.orderitem.OrderItemRepository;
import io.github.carloscardoso05.api.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Page<OrderDto> listOrders(Pageable pageable, Integer customerId) {
        var orders = customerId != null
                ? orderRepository.findAllByCustomerId(customerId, pageable)
                : orderRepository.findAll(pageable);
        var itemsByOrderId = loadItemsByOrderId(orders.getContent());
        return orders.map(order -> OrderDto.of(order, itemsByOrderId.getOrDefault(order.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public OrderDto findOrderById(Integer id) {
        var order = getOrderById(id);
        return OrderDto.of(order, orderItemRepository.findAllByOrderId(id));
    }

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request) {
        var customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException(Customer.class, request.customerId()));
        var order = new Order(customer, request.orderedAt());
        return OrderDto.of(orderRepository.save(order), List.of());
    }

    @Transactional
    public OrderDto updateOrder(Integer id, UpdateOrderRequest request) {
        var order = getOrderById(id);
        if (request.orderedAt() != null) {
            order.setOrderedAt(request.orderedAt());
        }
        return OrderDto.of(order, orderItemRepository.findAllByOrderId(id));
    }

    @Transactional
    public void deleteOrder(Integer id) {
        var order = getOrderById(id);
        var customer = order.getCustomer();
        for (var item : orderItemRepository.findAllByOrderId(id)) {
            customer.setBalance(customer.getBalance().add(item.getPrice()));
            var product = item.getProduct();
            product.setStock(product.getStock() + 1);
        }
        orderRepository.delete(order);
    }

    private Order getOrderById(Integer id) {
        return orderRepository.findById(id).orElseThrow(() -> new NotFoundException(Order.class, id));
    }

    private Map<Integer, List<OrderItem>> loadItemsByOrderId(List<Order> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        var orderIds = orders.stream().map(Order::getId).toList();
        return orderItemRepository.findAllByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
    }
}

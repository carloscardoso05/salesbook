package io.github.carloscardoso05.api.order;

import io.github.carloscardoso05.api.customer.Customer;
import io.github.carloscardoso05.api.customer.CustomerRepository;
import io.github.carloscardoso05.api.order.dto.CreateOrderRequest;
import io.github.carloscardoso05.api.order.dto.OrderDto;
import io.github.carloscardoso05.api.order.dto.UpdateOrderRequest;
import io.github.carloscardoso05.api.orderitem.OrderItem;
import io.github.carloscardoso05.api.orderitem.OrderItemRepository;
import io.github.carloscardoso05.api.product.Product;
import io.github.carloscardoso05.api.shared.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private Order order;
    private Product product;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        customer = new Customer("Carlos", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(customer, "id", 1);
        order = new Order(customer, Instant.parse("2026-08-10T12:00:00Z"));
        ReflectionTestUtils.setField(order, "id", 10);
        product = new Product("Coffee", 10);
        ReflectionTestUtils.setField(product, "id", 5);
        orderItem = new OrderItem(order, product, new BigDecimal("25.00"));
        ReflectionTestUtils.setField(orderItem, "id", 20);
    }

    @Test
    void listOrders() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findAll(pageable)).thenReturn(page);
        when(orderItemRepository.findAllByOrderIdIn(List.of(10))).thenReturn(List.of(orderItem));

        Page<OrderDto> result = orderService.listOrders(pageable, null);

        assertThat(result.getContent()).hasSize(1);
        var dto = result.getContent().getFirst();
        assertThat(dto.id()).isEqualTo(10);
        assertThat(dto.customerId()).isEqualTo(1);
        assertThat(dto.customerName()).isEqualTo("Carlos");
        assertThat(dto.totalPrice()).isEqualTo(new BigDecimal("25.00"));
        assertThat(dto.items()).hasSize(1);
        assertThat(dto.items().getFirst().id()).isEqualTo(20);
    }

    @Test
    void listOrders_byCustomer() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findAllByCustomerId(1, pageable)).thenReturn(page);
        when(orderItemRepository.findAllByOrderIdIn(List.of(10))).thenReturn(List.of(orderItem));

        Page<OrderDto> result = orderService.listOrders(pageable, 1);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().totalPrice()).isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    void listOrders_noItems() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findAll(pageable)).thenReturn(page);
        when(orderItemRepository.findAllByOrderIdIn(List.of(10))).thenReturn(List.of());

        Page<OrderDto> result = orderService.listOrders(pageable, null);

        assertThat(result.getContent()).hasSize(1);
        var dto = result.getContent().getFirst();
        assertThat(dto.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.items()).isEmpty();
    }

    @Test
    void findOrderById() {
        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(10)).thenReturn(List.of(orderItem));

        OrderDto result = orderService.findOrderById(10);

        assertThat(result.id()).isEqualTo(10);
        assertThat(result.totalPrice()).isEqualTo(new BigDecimal("25.00"));
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().productId()).isEqualTo(5);
        assertThat(result.items().getFirst().productName()).isEqualTo("Coffee");
        assertThat(result.items().getFirst().price()).isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    void findOrderById_notFound() {
        when(orderRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findOrderById(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Order for id 99 not found");
    }

    @Test
    void createOrder() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0, Order.class));

        OrderDto result = orderService.createOrder(new CreateOrderRequest(1, null));

        assertThat(result.customerId()).isEqualTo(1);
        assertThat(result.customerName()).isEqualTo("Carlos");
        assertThat(result.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void createOrder_customerNotFound() {
        when(customerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(99, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer for id 99 not found");
    }

    @Test
    void updateOrder() {
        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(10)).thenReturn(List.of(orderItem));
        var newOrderedAt = Instant.parse("2026-08-11T12:00:00Z");

        OrderDto result = orderService.updateOrder(10, new UpdateOrderRequest(newOrderedAt));

        assertThat(order.getOrderedAt()).isEqualTo(newOrderedAt);
        assertThat(result.orderedAt()).isEqualTo(newOrderedAt);
    }

    @Test
    void updateOrder_notFound() {
        when(orderRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrder(99, new UpdateOrderRequest(null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Order for id 99 not found");
    }

    @Test
    void deleteOrder_refundsBalanceAndStock() {
        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(10)).thenReturn(List.of(orderItem));

        orderService.deleteOrder(10);

        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("125.00"));
        assertThat(product.getStock()).isEqualTo(11);
        verify(orderRepository).delete(order);
    }

    @Test
    void deleteOrder_notFound() {
        when(orderRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Order for id 99 not found");
    }
}

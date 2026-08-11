package io.github.carloscardoso05.api.orderitem;

import io.github.carloscardoso05.api.customer.Customer;
import io.github.carloscardoso05.api.order.Order;
import io.github.carloscardoso05.api.order.OrderRepository;
import io.github.carloscardoso05.api.orderitem.dto.CreateOrderItemRequest;
import io.github.carloscardoso05.api.orderitem.dto.OrderItemDto;
import io.github.carloscardoso05.api.orderitem.dto.UpdateOrderItemRequest;
import io.github.carloscardoso05.api.product.Product;
import io.github.carloscardoso05.api.product.ProductRepository;
import io.github.carloscardoso05.api.shared.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderItemService orderItemService;

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
    void listItems() {
        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(10)).thenReturn(List.of(orderItem));

        List<OrderItemDto> result = orderItemService.listItems(10);

        assertThat(result).hasSize(1);
        var dto = result.getFirst();
        assertThat(dto.id()).isEqualTo(20);
        assertThat(dto.orderId()).isEqualTo(10);
        assertThat(dto.productId()).isEqualTo(5);
        assertThat(dto.productName()).isEqualTo("Coffee");
        assertThat(dto.price()).isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    void listItems_orderNotFound() {
        when(orderRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.listItems(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Order for id 99 not found");
    }

    @Test
    void createItem_deductsBalanceAndStock() {
        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(productRepository.findById(5)).thenReturn(Optional.of(product));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0, OrderItem.class));

        OrderItemDto result = orderItemService.createItem(10, new CreateOrderItemRequest(5, new BigDecimal("25.00")));

        assertThat(result.productId()).isEqualTo(5);
        assertThat(result.price()).isEqualTo(new BigDecimal("25.00"));
        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("75.00"));
        assertThat(product.getStock()).isEqualTo(9);
    }

    @Test
    void createItem_orderNotFound() {
        when(orderRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.createItem(99, new CreateOrderItemRequest(5, new BigDecimal("25.00"))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Order for id 99 not found");
    }

    @Test
    void createItem_productNotFound() {
        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.createItem(10, new CreateOrderItemRequest(99, new BigDecimal("25.00"))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product for id 99 not found");
    }

    @Test
    void createItem_insufficientStock() {
        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(productRepository.findById(5)).thenReturn(Optional.of(product));
        product.setStock(0);

        assertThatThrownBy(() -> orderItemService.createItem(10, new CreateOrderItemRequest(5, new BigDecimal("25.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product 'Coffee' has insufficient stock.");
        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("100.00"));
        assertThat(product.getStock()).isZero();
    }

    @Test
    void updateItem_priceIncreased() {
        when(orderItemRepository.findById(20)).thenReturn(Optional.of(orderItem));

        OrderItemDto result = orderItemService.updateItem(10, 20, new UpdateOrderItemRequest(new BigDecimal("40.00")));

        assertThat(orderItem.getPrice()).isEqualTo(new BigDecimal("40.00"));
        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("85.00"));
        assertThat(result.price()).isEqualTo(new BigDecimal("40.00"));
    }

    @Test
    void updateItem_priceDecreased() {
        when(orderItemRepository.findById(20)).thenReturn(Optional.of(orderItem));

        OrderItemDto result = orderItemService.updateItem(10, 20, new UpdateOrderItemRequest(new BigDecimal("10.00")));

        assertThat(orderItem.getPrice()).isEqualTo(new BigDecimal("10.00"));
        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("115.00"));
        assertThat(result.price()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void updateItem_noPriceKeepsBalance() {
        when(orderItemRepository.findById(20)).thenReturn(Optional.of(orderItem));

        OrderItemDto result = orderItemService.updateItem(10, 20, new UpdateOrderItemRequest(null));

        assertThat(orderItem.getPrice()).isEqualTo(new BigDecimal("25.00"));
        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.price()).isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    void updateItem_itemNotFound() {
        when(orderItemRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.updateItem(10, 99, new UpdateOrderItemRequest(new BigDecimal("40.00"))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("OrderItem for id 99 not found");
    }

    @Test
    void updateItem_itemNotInOrder() {
        when(orderItemRepository.findById(20)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> orderItemService.updateItem(99, 20, new UpdateOrderItemRequest(new BigDecimal("40.00"))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("OrderItem for id 20 not found");
    }

    @Test
    void deleteItem_refundsBalanceAndStock() {
        when(orderItemRepository.findById(20)).thenReturn(Optional.of(orderItem));

        orderItemService.deleteItem(10, 20);

        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("125.00"));
        assertThat(product.getStock()).isEqualTo(11);
        verify(orderItemRepository).delete(orderItem);
    }

    @Test
    void deleteItem_itemNotFound() {
        when(orderItemRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.deleteItem(10, 99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("OrderItem for id 99 not found");
    }
}

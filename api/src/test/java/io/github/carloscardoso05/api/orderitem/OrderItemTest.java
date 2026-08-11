package io.github.carloscardoso05.api.orderitem;

import io.github.carloscardoso05.api.customer.Customer;
import io.github.carloscardoso05.api.order.Order;
import io.github.carloscardoso05.api.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OrderItemTest {
    private Order order;
    private Product product;

    @BeforeEach
    void setUp() {
        order = new Order(new Customer("Carlos", new BigDecimal("100.00")), Instant.parse("2026-08-10T12:00:00Z"));
        product = new Product("Coffee", 10);
    }

    @Test
    void testConstructor() {
        var orderItem = new OrderItem(order, product, new BigDecimal("25.00"));
        assertThat(orderItem.getOrder()).isSameAs(order);
        assertThat(orderItem.getProduct()).isSameAs(product);
        assertThat(orderItem.getPrice()).isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    void setOrder_failsOn_null() {
        assertThatIllegalArgumentException().isThrownBy(() -> new OrderItem(null, product, new BigDecimal("25.00")));
    }

    @Test
    void setProduct_failsOn_null() {
        assertThatIllegalArgumentException().isThrownBy(() -> new OrderItem(order, null, new BigDecimal("25.00")));
    }

    @Test
    void setPrice_failsOn_null() {
        assertThatIllegalArgumentException().isThrownBy(() -> new OrderItem(order, product, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-100.00"})
    void setPrice_failsOn_negativeValue(String price) {
        assertThatIllegalArgumentException().isThrownBy(() -> new OrderItem(order, product, new BigDecimal(price)));
    }
}

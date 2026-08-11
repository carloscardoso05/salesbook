package io.github.carloscardoso05.api.order;

import io.github.carloscardoso05.api.customer.Customer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OrderTest {
    private final Instant orderedAt = Instant.parse("2026-08-10T12:00:00Z");

    @Test
    void testConstructor() {
        var customer = new Customer("Carlos", new BigDecimal("100.00"));
        var order = new Order(customer, orderedAt);
        assertThat(order.getCustomer()).isSameAs(customer);
        assertThat(order.getOrderedAt()).isEqualTo(orderedAt);
    }

    @Test
    void setCustomer_failsOn_null() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Order(null, orderedAt));
    }

    @Test
    void setOrderedAt_failsOn_null() {
        var customer = new Customer("Carlos", new BigDecimal("100.00"));
        assertThatIllegalArgumentException().isThrownBy(() -> new Order(customer, null));
    }
}

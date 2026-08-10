package io.github.carloscardoso05.api.payment;

import io.github.carloscardoso05.api.customer.Customer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PaymentTest {

    private final Customer customer = new Customer("Carlos", new BigDecimal("100.00"));
    private final Instant paidAt = Instant.parse("2026-08-10T12:00:00Z");

    @Test
    void testConstructor() {
        var payment = new Payment(new BigDecimal("50.00"), customer, paidAt);

        assertThat(payment.getValue()).isEqualTo(new BigDecimal("50.00"));
        assertThat(payment.getCustomer()).isSameAs(customer);
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
    }

    @Test
    void setValue_failsOn_null() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Payment(null, customer, paidAt));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-100.00"})
    void setValue_failsOn_nonPositive(String value) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Payment(new BigDecimal(value), customer, paidAt));
    }

    @Test
    void setCustomer_failsOn_null() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Payment(new BigDecimal("50.00"), null, paidAt));
    }

    @Test
    void setPaidAt_failsOn_null() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Payment(new BigDecimal("50.00"), customer, null));
    }
}

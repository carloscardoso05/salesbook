package io.github.carloscardoso05.api.adjustment;

import io.github.carloscardoso05.api.customer.Customer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BalanceAdjustmentTest {
    private final Customer customer = new Customer("Carlos", new BigDecimal("100.00"));

    @Test
    void testConstructor() {
        var adjustment = new BalanceAdjustment(customer, new BigDecimal("-50.00"));

        assertThat(adjustment.getCustomer()).isSameAs(customer);
        assertThat(adjustment.getValue()).isEqualTo(new BigDecimal("-50.00"));
        assertThat(adjustment.getAdjustedAt()).isNotNull();
    }

    @Test
    void constructor_failsOn_nullCustomer() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BalanceAdjustment(null, new BigDecimal("-50.00")));
    }

    @Test
    void constructor_failsOn_nullValue() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BalanceAdjustment(customer, null));
    }
}

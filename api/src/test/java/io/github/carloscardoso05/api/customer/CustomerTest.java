package io.github.carloscardoso05.api.customer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CustomerTest {
    @Test
    void testConstructor() {
        var customer = new Customer("Carlos", new BigDecimal("12.5"));
        assertThat(customer.getName()).isEqualTo("Carlos");
        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("12.5"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void setName_failsOn_blankValues(String name) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Customer(name, new BigDecimal("12.5")));
    }

    @Test
    void setBalance_failsOn_negativeValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Customer("Carlos", null));
    }
}
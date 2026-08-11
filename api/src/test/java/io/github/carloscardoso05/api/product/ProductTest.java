package io.github.carloscardoso05.api.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProductTest {
    @Test
    void testConstructor() {
        var product = new Product("Coffee", 10);
        assertThat(product.getName()).isEqualTo("Coffee");
        assertThat(product.getStock()).isEqualTo(10);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void setName_failsOn_blankValues(String name) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Product(name, 10));
    }

    @Test
    void setStock_failsOn_null() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Product("Coffee", null));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100})
    void setStock_failsOn_negativeValue(int stock) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Product("Coffee", stock));
    }
}

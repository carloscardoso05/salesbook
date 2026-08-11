package io.github.carloscardoso05.api.adjustment;

import io.github.carloscardoso05.api.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "balance_adjustments")
public class BalanceAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull
    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @NotNull
    @Column(name = "adjusted_at", nullable = false)
    private Instant adjustedAt;

    protected BalanceAdjustment() {
    }

    public BalanceAdjustment(Customer customer, BigDecimal value) {
        setCustomer(customer);
        setValue(value);
        this.adjustedAt = Instant.now();
    }

    private void setCustomer(Customer customer) {
        Assert.notNull(customer, "Customer cannot be null");
        this.customer = customer;
    }

    private void setValue(BigDecimal value) {
        Assert.notNull(value, "Value cannot be null");
        this.value = value;
    }
}

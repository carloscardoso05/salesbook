package io.github.carloscardoso05.api.payment;

import io.github.carloscardoso05.api.customer.Customer;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull
    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    protected Payment() {
    }

    public Payment(BigDecimal value, Customer customer, Instant paidAt) {
        setValue(value);
        setCustomer(customer);
        setPaidAt(paidAt);
    }

    public void setValue(BigDecimal value) {
        Assert.notNull(value, "Value cannot be null");
        Assert.isTrue(value.compareTo(BigDecimal.ZERO) > 0, "Value cannot be negative");
        this.value = value;
    }

    public void setCustomer(Customer customer) {
        Assert.notNull(customer, "Customer cannot be null");
        this.customer = customer;
    }

    public void setPaidAt(Instant paidAt) {
        Assert.notNull(paidAt, "Paid at cannot be null");
        this.paidAt = paidAt;
    }
}
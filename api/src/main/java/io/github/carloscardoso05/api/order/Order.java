package io.github.carloscardoso05.api.order;

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

import java.time.Instant;

@Getter
@Entity
@Table(name = "orders")
public class Order {
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
    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt;

    protected Order() {
    }

    public Order(Customer customer, Instant orderedAt) {
        setCustomer(customer);
        setOrderedAt(orderedAt);
    }

    public void setCustomer(Customer customer) {
        Assert.notNull(customer, "Customer cannot be null");
        this.customer = customer;
    }

    public void setOrderedAt(Instant orderedAt) {
        Assert.notNull(orderedAt, "Ordered at cannot be null");
        this.orderedAt = orderedAt;
    }
}

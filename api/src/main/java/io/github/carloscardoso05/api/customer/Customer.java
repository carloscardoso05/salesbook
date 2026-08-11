package io.github.carloscardoso05.api.customer;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @Version
    @Column(name = "version", nullable = false)
    private Instant version;

    protected Customer() {
    }

    public Customer(String name, BigDecimal balance) {
        setName(name);
        setBalance(balance);
    }

    public void setName(String name) {
        Assert.hasText(name, "Name must not be empty");
        this.name = name;
    }

    public void setBalance(BigDecimal balance) {
        Assert.notNull(balance, "Balance must not be null");
        this.balance = balance;
    }

    public void addToBalance(BigDecimal delta) {
        Assert.notNull(delta, "Delta cannot be null");
        this.balance = this.balance.add(delta);
    }
}

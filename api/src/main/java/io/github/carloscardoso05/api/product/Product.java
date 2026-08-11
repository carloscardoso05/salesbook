package io.github.carloscardoso05.api.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.util.Assert;

import java.time.Instant;

@Getter
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Version
    @Column(name = "version", nullable = false)
    private Instant version;

    protected Product() {
    }

    public Product(String name, Integer stock) {
        setName(name);
        setStock(stock);
    }

    public void setName(String name) {
        Assert.hasText(name, "Name must not be empty");
        this.name = name;
    }

    public void setStock(Integer stock) {
        Assert.notNull(stock, "Stock must not be null");
        Assert.isTrue(stock >= 0, "Stock cannot be negative");
        this.stock = stock;
    }

    public void addToStock(Integer delta) {
        Assert.notNull(delta, "Delta cannot be null");
        this.stock = this.stock + delta;
    }
}

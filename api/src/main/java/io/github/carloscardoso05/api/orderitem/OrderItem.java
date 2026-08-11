package io.github.carloscardoso05.api.orderitem;

import io.github.carloscardoso05.api.order.Order;
import io.github.carloscardoso05.api.product.Product;
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

@Getter
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    protected OrderItem() {
    }

    public OrderItem(Order order, Product product, BigDecimal price) {
        setOrder(order);
        setProduct(product);
        setPrice(price);
    }

    public void setOrder(Order order) {
        Assert.notNull(order, "Order cannot be null");
        this.order = order;
    }

    public void setProduct(Product product) {
        Assert.notNull(product, "Product cannot be null");
        this.product = product;
    }

    public void setPrice(BigDecimal price) {
        Assert.notNull(price, "Price cannot be null");
        Assert.isTrue(price.compareTo(BigDecimal.ZERO) >= 0, "Price cannot be negative");
        this.price = price;
    }
}

package io.github.carloscardoso05.api.orderitem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findAllByOrderId(Integer orderId);

    List<OrderItem> findAllByOrderIdIn(Collection<Integer> orderIds);

    Optional<OrderItem> findByIdAndOrderId(Integer id, Integer orderId);
}

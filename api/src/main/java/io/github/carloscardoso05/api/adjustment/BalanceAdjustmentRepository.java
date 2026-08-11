package io.github.carloscardoso05.api.adjustment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceAdjustmentRepository extends JpaRepository<BalanceAdjustment, Integer> {
    Page<BalanceAdjustment> findAllByCustomerId(Integer customerId, Pageable pageable);
}

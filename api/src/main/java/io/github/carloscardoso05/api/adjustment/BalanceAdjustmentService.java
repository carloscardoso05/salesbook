package io.github.carloscardoso05.api.adjustment;

import io.github.carloscardoso05.api.adjustment.dto.BalanceAdjustmentDto;
import io.github.carloscardoso05.api.customer.Customer;
import io.github.carloscardoso05.api.customer.CustomerRepository;
import io.github.carloscardoso05.api.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BalanceAdjustmentService {
    private final BalanceAdjustmentRepository balanceAdjustmentRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Page<BalanceAdjustmentDto> listAdjustments(Pageable pageable, Integer customerId) {
        if (customerId != null && !customerRepository.existsById(customerId)) {
            throw new NotFoundException(Customer.class, customerId);
        }
        var adjustments = customerId != null
                ? balanceAdjustmentRepository.findAllByCustomerId(customerId, pageable)
                : balanceAdjustmentRepository.findAll(pageable);
        return adjustments.map(BalanceAdjustmentDto::of);
    }

    @Transactional(readOnly = true)
    public BalanceAdjustmentDto findAdjustmentById(Integer id) {
        return BalanceAdjustmentDto.of(getAdjustmentById(id));
    }

    private BalanceAdjustment getAdjustmentById(Integer id) {
        return balanceAdjustmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(BalanceAdjustment.class, id));
    }
}

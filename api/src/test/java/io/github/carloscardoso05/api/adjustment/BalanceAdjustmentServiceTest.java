package io.github.carloscardoso05.api.adjustment;

import io.github.carloscardoso05.api.adjustment.dto.BalanceAdjustmentDto;
import io.github.carloscardoso05.api.customer.Customer;
import io.github.carloscardoso05.api.customer.CustomerRepository;
import io.github.carloscardoso05.api.shared.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceAdjustmentServiceTest {

    @Mock
    private BalanceAdjustmentRepository balanceAdjustmentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private BalanceAdjustmentService balanceAdjustmentService;

    private Customer customer;
    private BalanceAdjustment adjustment;
    private Instant adjustedAt;

    @BeforeEach
    void setUp() {
        customer = new Customer("Carlos", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(customer, "id", 1);
        adjustedAt = Instant.parse("2026-08-11T12:00:00Z");
        adjustment = new BalanceAdjustment(customer, new BigDecimal("-50.00"));
        ReflectionTestUtils.setField(adjustment, "adjustedAt", adjustedAt);
        ReflectionTestUtils.setField(adjustment, "id", 10);
    }

    @Test
    void listAdjustments() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(adjustment), pageable, 1);
        when(balanceAdjustmentRepository.findAll(pageable)).thenReturn(page);

        Page<BalanceAdjustmentDto> result = balanceAdjustmentService.listAdjustments(pageable, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().value()).isEqualTo(new BigDecimal("-50.00"));
    }

    @Test
    void listAdjustments_byCustomer() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(adjustment), pageable, 1);
        when(customerRepository.existsById(1)).thenReturn(true);
        when(balanceAdjustmentRepository.findAllByCustomerId(1, pageable)).thenReturn(page);

        Page<BalanceAdjustmentDto> result = balanceAdjustmentService.listAdjustments(pageable, 1);

        assertThat(result.getContent()).hasSize(1);
        var dto = result.getContent().getFirst();
        assertThat(dto.id()).isEqualTo(10);
        assertThat(dto.value()).isEqualTo(new BigDecimal("-50.00"));
        assertThat(dto.customerId()).isEqualTo(1);
        assertThat(dto.customerName()).isEqualTo("Carlos");
        assertThat(dto.adjustedAt()).isEqualTo(adjustedAt);
    }

    @Test
    void listAdjustments_customerNotFound() {
        when(customerRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> balanceAdjustmentService.listAdjustments(PageRequest.of(0, 10), 99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer for id 99 not found");
    }

    @Test
    void findAdjustmentById() {
        when(balanceAdjustmentRepository.findById(10)).thenReturn(Optional.of(adjustment));

        BalanceAdjustmentDto result = balanceAdjustmentService.findAdjustmentById(10);

        assertThat(result.id()).isEqualTo(10);
        assertThat(result.value()).isEqualTo(new BigDecimal("-50.00"));
        assertThat(result.customerId()).isEqualTo(1);
        assertThat(result.customerName()).isEqualTo("Carlos");
        assertThat(result.adjustedAt()).isEqualTo(adjustedAt);
    }

    @Test
    void findAdjustmentById_notFound() {
        when(balanceAdjustmentRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> balanceAdjustmentService.findAdjustmentById(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("BalanceAdjustment for id 99 not found");
    }
}

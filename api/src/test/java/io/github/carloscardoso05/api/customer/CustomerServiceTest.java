package io.github.carloscardoso05.api.customer;

import io.github.carloscardoso05.api.adjustment.BalanceAdjustment;
import io.github.carloscardoso05.api.adjustment.BalanceAdjustmentRepository;
import io.github.carloscardoso05.api.customer.dto.CreateCustomerRequest;
import io.github.carloscardoso05.api.customer.dto.CustomerDto;
import io.github.carloscardoso05.api.customer.dto.UpdateCustomerRequest;
import io.github.carloscardoso05.api.shared.DuplicateException;
import io.github.carloscardoso05.api.shared.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BalanceAdjustmentRepository balanceAdjustmentRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer("Carlos", new BigDecimal("100.00"));
    }

    @Test
    void listCustomers() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(customer), pageable, 1);
        when(customerRepository.findAll(pageable)).thenReturn(page);

        Page<CustomerDto> result = customerService.listCustomers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Carlos");
    }

    @Test
    void findCustomerById() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        CustomerDto result = customerService.findCustomerById(1);

        assertThat(result.name()).isEqualTo("Carlos");
        assertThat(result.balance()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void findCustomerById_notFound() {
        when(customerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findCustomerById(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer for id 99 not found");
    }

    @Test
    void createCustomer() {
        var request = new CreateCustomerRequest("Maria", new BigDecimal("50.00"));
        when(customerRepository.existsByNameIgnoreCase("Maria")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0, Customer.class));

        CustomerDto result = customerService.createCustomer(request);

        assertThat(result.name()).isEqualTo("Maria");
        assertThat(result.balance()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void createCustomer_existingName() {
        var request = new CreateCustomerRequest("Carlos", new BigDecimal("50.00"));
        when(customerRepository.existsByNameIgnoreCase("Carlos")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("Customer with name 'Carlos' already exists.");
    }

    @Test
    void updateCustomer() {
        when(customerRepository.existsByNameIgnoreCaseAndIdNot(null, 1)).thenReturn(false);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(balanceAdjustmentRepository.save(any(BalanceAdjustment.class))).thenAnswer(inv -> inv.getArgument(0, BalanceAdjustment.class));

        var request = new UpdateCustomerRequest(null, new BigDecimal("200.00"));
        CustomerDto result = customerService.updateCustomer(1, request);

        assertThat(result.balance()).isEqualTo(new BigDecimal("200.00"));
        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("200.00"));

        var captor = ArgumentCaptor.forClass(BalanceAdjustment.class);
        verify(balanceAdjustmentRepository).save(captor.capture());
        var adjustment = captor.getValue();
        assertThat(adjustment.getCustomer()).isSameAs(customer);
        assertThat(adjustment.getValue()).isEqualTo(new BigDecimal("100.00"));
        assertThat(adjustment.getAdjustedAt()).isNotNull();
    }

    @Test
    void updateCustomer_sameBalance_doesNotCreateAdjustment() {
        when(customerRepository.existsByNameIgnoreCaseAndIdNot(null, 1)).thenReturn(false);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        var request = new UpdateCustomerRequest(null, new BigDecimal("100.00"));
        customerService.updateCustomer(1, request);

        verify(balanceAdjustmentRepository, never()).save(any());
    }

    @Test
    void updateCustomer_withoutBalance_doesNotCreateAdjustment() {
        when(customerRepository.existsByNameIgnoreCaseAndIdNot("Carlos Updated", 1)).thenReturn(false);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        var request = new UpdateCustomerRequest("Carlos Updated", null);
        customerService.updateCustomer(1, request);

        verify(balanceAdjustmentRepository, never()).save(any());
    }

    @Test
    void updateCustomer_idNotFound() {
        when(customerRepository.existsByNameIgnoreCaseAndIdNot("Anyone", 99)).thenReturn(false);
        when(customerRepository.findById(99)).thenReturn(Optional.empty());

        var request = new UpdateCustomerRequest("Anyone", null);

        assertThatThrownBy(() -> customerService.updateCustomer(99, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer for id 99 not found");
    }

    @Test
    void updateCustomer_existingName() {
        var request = new UpdateCustomerRequest("Maria", null);
        when(customerRepository.existsByNameIgnoreCaseAndIdNot("Maria", 1)).thenReturn(true);

        assertThatThrownBy(() -> customerService.updateCustomer(1, request))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("Customer with name 'Maria' already exists.");
    }

    @Test
    void deleteCustomer() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(1);

        verify(customerRepository).delete(customer);
    }

    @Test
    void deleteCustomer_notFound() {
        when(customerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.deleteCustomer(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer for id 99 not found");
    }
}

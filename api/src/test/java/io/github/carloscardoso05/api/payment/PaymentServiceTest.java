package io.github.carloscardoso05.api.payment;

import io.github.carloscardoso05.api.customer.Customer;
import io.github.carloscardoso05.api.customer.CustomerRepository;
import io.github.carloscardoso05.api.payment.dto.CreatePaymentRequest;
import io.github.carloscardoso05.api.payment.dto.PaymentDto;
import io.github.carloscardoso05.api.payment.dto.UpdatePaymentRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Customer customer;
    private Payment payment;
    private Instant paidAt;

    @BeforeEach
    void setUp() {
        customer = new Customer("Carlos", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(customer, "id", 1);
        paidAt = Instant.parse("2026-08-10T12:00:00Z");
        payment = new Payment(new BigDecimal("50.00"), customer, paidAt);
        ReflectionTestUtils.setField(payment, "id", 10);
    }

    @Test
    void listPayments() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(payment), pageable, 1);
        when(paymentRepository.findAll(pageable)).thenReturn(page);

        Page<PaymentDto> result = paymentService.listPayments(pageable, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().value()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void listPayments_byCustomer() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(payment), pageable, 1);
        when(customerRepository.existsById(1)).thenReturn(true);
        when(paymentRepository.findAllByCustomerId(1, pageable)).thenReturn(page);

        Page<PaymentDto> result = paymentService.listPayments(pageable, 1);

        assertThat(result.getContent()).hasSize(1);
        var dto = result.getContent().getFirst();
        assertThat(dto.id()).isEqualTo(10);
        assertThat(dto.value()).isEqualTo(new BigDecimal("50.00"));
        assertThat(dto.customerId()).isEqualTo(1);
        assertThat(dto.customerName()).isEqualTo("Carlos");
        assertThat(dto.paidAt()).isEqualTo(paidAt);
    }

    @Test
    void listPayments_customerNotFound() {
        when(customerRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> paymentService.listPayments(PageRequest.of(0, 10), 99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer for id 99 not found");
    }

    @Test
    void findPaymentById() {
        when(paymentRepository.findById(10)).thenReturn(Optional.of(payment));

        PaymentDto result = paymentService.findPaymentById(10);

        assertThat(result.id()).isEqualTo(10);
        assertThat(result.value()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.customerId()).isEqualTo(1);
        assertThat(result.customerName()).isEqualTo("Carlos");
        assertThat(result.paidAt()).isEqualTo(paidAt);
    }

    @Test
    void findPaymentById_notFound() {
        when(paymentRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findPaymentById(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Payment for id 99 not found");
    }

    @Test
    void createPayment() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0, Payment.class));
        var request = new CreatePaymentRequest(1, new BigDecimal("50.00"), null);

        var result = paymentService.createPayment(request);

        assertThat(result.value()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.customerId()).isEqualTo(1);
        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("150.00"));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_customerNotFound() {
        when(customerRepository.findById(99)).thenReturn(Optional.empty());
        var request = new CreatePaymentRequest(99, new BigDecimal("50.00"), null);

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer for id 99 not found");
    }

    @Test
    void updatePayment_valueChanged() {
        when(paymentRepository.findById(10)).thenReturn(Optional.of(payment));
        var request = new UpdatePaymentRequest(new BigDecimal("80.00"), null);

        var result = paymentService.updatePayment(10, request);

        assertThat(payment.getValue()).isEqualTo(new BigDecimal("80.00"));
        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("130.00"));
        assertThat(result.value()).isEqualTo(new BigDecimal("80.00"));
    }

    @Test
    void updatePayment_onlyPaidAt() {
        when(paymentRepository.findById(10)).thenReturn(Optional.of(payment));
        var newPaidAt = Instant.parse("2026-08-11T12:00:00Z");
        var request = new UpdatePaymentRequest(null, newPaidAt);

        var result = paymentService.updatePayment(10, request);

        assertThat(payment.getPaidAt()).isEqualTo(newPaidAt);
        assertThat(payment.getValue()).isEqualTo(new BigDecimal("50.00"));
        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.paidAt()).isEqualTo(newPaidAt);
    }

    @Test
    void updatePayment_notFound() {
        when(paymentRepository.findById(99)).thenReturn(Optional.empty());
        var request = new UpdatePaymentRequest(null, null);

        assertThatThrownBy(() -> paymentService.updatePayment(99, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Payment for id 99 not found");
    }

    @Test
    void deletePayment() {
        when(paymentRepository.findById(10)).thenReturn(Optional.of(payment));

        paymentService.deletePayment(10);

        assertThat(customer.getBalance()).isEqualTo(new BigDecimal("50.00"));
        verify(paymentRepository).delete(payment);
    }

    @Test
    void deletePayment_notFound() {
        when(paymentRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.deletePayment(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Payment for id 99 not found");
    }
}

package io.github.carloscardoso05.api.payment;

import io.github.carloscardoso05.api.customer.CustomerRepository;
import io.github.carloscardoso05.api.payment.dtos.CreatePaymentRequest;
import io.github.carloscardoso05.api.payment.dtos.PaymentDto;
import io.github.carloscardoso05.api.payment.dtos.UpdatePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<PaymentDto> listPaymentsByCustomer(Integer customerId) {
        return paymentRepository.findAllByCustomerId(customerId).stream()
                .map(PaymentDto::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentDto findPaymentById(Integer id) {
        return PaymentDto.of(getPaymentById(id));
    }

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request) {
        var customer = customerRepository.findById(request.customerId()).orElseThrow();
        var payment = new Payment(request.value(), customer, request.paidAt());
        customer.setBalance(customer.getBalance().add(request.value()));
        return PaymentDto.of(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentDto updatePayment(Integer id, UpdatePaymentRequest request) {
        var payment = getPaymentById(id);
        if (request.value() != null) {
            var difference = request.value().subtract(payment.getValue());
            var customer = payment.getCustomer();
            customer.setBalance(customer.getBalance().add(difference));
            payment.setValue(request.value());
        }
        if (request.paidAt() != null) {
            payment.setPaidAt(request.paidAt());
        }
        return PaymentDto.of(payment);
    }

    @Transactional
    public void deletePayment(Integer id) {
        var payment = getPaymentById(id);
        var customer = payment.getCustomer();
        customer.setBalance(customer.getBalance().subtract(payment.getValue()));
        paymentRepository.delete(payment);
    }

    private Payment getPaymentById(Integer id) {
        return paymentRepository.findById(id).orElseThrow();
    }
}

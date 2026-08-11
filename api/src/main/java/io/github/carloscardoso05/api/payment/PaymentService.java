package io.github.carloscardoso05.api.payment;

import io.github.carloscardoso05.api.customer.Customer;
import io.github.carloscardoso05.api.customer.CustomerRepository;
import io.github.carloscardoso05.api.payment.dto.CreatePaymentRequest;
import io.github.carloscardoso05.api.shared.NotFoundException;
import io.github.carloscardoso05.api.payment.dto.PaymentDto;
import io.github.carloscardoso05.api.payment.dto.UpdatePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Page<PaymentDto> listPayments(Pageable pageable, Integer customerId) {
        if (customerId != null && !customerRepository.existsById(customerId)) {
            throw new NotFoundException(Customer.class, customerId);
        }
        var payments = customerId != null
                ? paymentRepository.findAllByCustomerId(customerId, pageable)
                : paymentRepository.findAll(pageable);
        return payments.map(PaymentDto::of);
    }

    @Transactional(readOnly = true)
    public PaymentDto findPaymentById(Integer id) {
        return PaymentDto.of(getPaymentById(id));
    }

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request) {
        var customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException(Customer.class, request.customerId()));
        var payment = new Payment(request.value(), customer, request.paidAt());
        customer.addToBalance(request.value());
        return PaymentDto.of(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentDto updatePayment(Integer id, UpdatePaymentRequest request) {
        var payment = getPaymentById(id);
        if (request.value() != null) {
            var difference = request.value().subtract(payment.getValue());
            payment.getCustomer().addToBalance(difference);
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
        payment.getCustomer().addToBalance(payment.getValue().negate());
        paymentRepository.delete(payment);
    }

    private Payment getPaymentById(Integer id) {
        return paymentRepository.findById(id).orElseThrow(() -> new NotFoundException(Payment.class, id));
    }
}

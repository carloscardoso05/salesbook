package io.github.carloscardoso05.api.customer;

import io.github.carloscardoso05.api.customer.dto.CreateCustomerRequest;
import io.github.carloscardoso05.api.customer.dto.CustomerDto;
import io.github.carloscardoso05.api.customer.dto.UpdateCustomerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;

    public Page<CustomerDto> listCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable).map(CustomerDto::of);
    }

    public CustomerDto findCustomerById(Integer id) {
        return CustomerDto.of(getCustomerById(id));
    }

    public CustomerDto createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("Customer with name '%s' already exists.".formatted(request.name()));
        }
        var customer = new Customer(request.name(), request.balance());
        return CustomerDto.of(customerRepository.save(customer));
    }

    public CustomerDto updateCustomer(Integer id, UpdateCustomerRequest request) {
        if (customerRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException("Customer with name '%s' already exists.".formatted(request.name()));
        }
        var customer = getCustomerById(id);
        if (StringUtils.hasText(request.name())) {
            customer.setName(request.name());
        }
        if (request.balance() != null) {
            customer.setBalance(request.balance());
        }
        return CustomerDto.of(customerRepository.save(customer));
    }

    public void deleteCustomer(Integer id) {
        customerRepository.deleteById(id);
    }

    private Customer getCustomerById(Integer id) {
        return customerRepository.findById(id).orElseThrow();
    }
}

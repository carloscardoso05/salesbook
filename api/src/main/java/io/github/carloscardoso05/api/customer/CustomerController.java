package io.github.carloscardoso05.api.customer;

import io.github.carloscardoso05.api.customer.dto.CreateCustomerRequest;
import io.github.carloscardoso05.api.customer.dto.CustomerDto;
import io.github.carloscardoso05.api.customer.dto.UpdateCustomerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer CRUD operations")
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "List customers", description = "Returns a paginated list of all customers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of customers")
    })
    public Page<CustomerDto> listCustomers(@ParameterObject Pageable pageable) {
        return customerService.listCustomers(pageable);
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Find customer by ID", description = "Returns a single customer by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public CustomerDto findCustomerById(@Parameter(description = "Customer ID", example = "1") @PathVariable Integer id) {
        return customerService.findCustomerById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create customer", description = "Creates a new customer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer created"),
            @ApiResponse(responseCode = "400", description = "Customer name already exists")
    })
    public CustomerDto createCustomer(@RequestBody @Valid CreateCustomerRequest request) {
        return customerService.createCustomer(request);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update customer", description = "Updates an existing customer. Only non-null fields are applied.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer updated"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "400", description = "Customer name already exists")
    })
    public CustomerDto updateCustomer(
            @Parameter(description = "Customer ID", example = "1") @PathVariable Integer id,
            @RequestBody @Valid UpdateCustomerRequest request) {
        return customerService.updateCustomer(id, request);
    }

    @DeleteMapping(path = "/{id}")
    @Operation(summary = "Delete customer", description = "Deletes a customer by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer deleted"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public void deleteCustomer(
            @Parameter(description = "Customer ID", example = "1") @PathVariable Integer id) {
        customerService.deleteCustomer(id);
    }
}

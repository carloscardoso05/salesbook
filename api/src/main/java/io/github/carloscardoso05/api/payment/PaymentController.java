package io.github.carloscardoso05.api.payment;

import io.github.carloscardoso05.api.payment.dtos.CreatePaymentRequest;
import io.github.carloscardoso05.api.payment.dtos.PaymentDto;
import io.github.carloscardoso05.api.payment.dtos.UpdatePaymentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment CRUD operations")
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List payments by customer", description = "Returns all payments for a given customer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of payments for the customer")
    })
    public List<PaymentDto> listPaymentsByCustomer(
            @Parameter(description = "Customer ID", example = "1") @PathVariable Integer customerId) {
        return paymentService.listPaymentsByCustomer(customerId);
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Find payment by ID", description = "Returns a single payment by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public PaymentDto findPaymentById(
            @Parameter(description = "Payment ID", example = "1") @PathVariable Integer id) {
        return paymentService.findPaymentById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create payment", description = "Creates a new payment for a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment created"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public PaymentDto createPayment(@RequestBody @Valid CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update payment", description = "Updates an existing payment. Only non-null fields are applied.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment updated"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public PaymentDto updatePayment(
            @Parameter(description = "Payment ID", example = "1") @PathVariable Integer id,
            @RequestBody @Valid UpdatePaymentRequest request) {
        return paymentService.updatePayment(id, request);
    }

    @DeleteMapping(path = "/{id}")
    @Operation(summary = "Delete payment", description = "Deletes a payment by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment deleted"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public void deletePayment(
            @Parameter(description = "Payment ID", example = "1") @PathVariable Integer id) {
        paymentService.deletePayment(id);
    }
}

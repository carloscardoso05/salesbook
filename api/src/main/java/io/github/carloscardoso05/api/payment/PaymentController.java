package io.github.carloscardoso05.api.payment;

import io.github.carloscardoso05.api.payment.dto.CreatePaymentRequest;
import io.github.carloscardoso05.api.payment.dto.PaymentDto;
import io.github.carloscardoso05.api.payment.dto.UpdatePaymentRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment CRUD operations")
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "List payments", description = "Returns a paginated list of all payments, or filtered by a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of payments"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public Page<PaymentDto> listPayments(
            @Parameter(description = "Customer ID to filter by", example = "1")
            @RequestParam(value = "customerId", required = false) Integer customerId,
            @ParameterObject Pageable pageable) {
        return paymentService.listPayments(pageable, customerId);
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
            @ApiResponse(responseCode = "201", description = "Payment created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<PaymentDto> createPayment(@RequestBody @Valid CreatePaymentRequest request) {
        var dto = paymentService.createPayment(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(dto.id())
                .toUri();
        return ResponseEntity.created(location).body(dto);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update payment", description = "Updates an existing payment. Only non-null fields are applied.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Concurrent modification")
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
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Concurrent modification")
    })
    public void deletePayment(
            @Parameter(description = "Payment ID", example = "1") @PathVariable Integer id) {
        paymentService.deletePayment(id);
    }
}

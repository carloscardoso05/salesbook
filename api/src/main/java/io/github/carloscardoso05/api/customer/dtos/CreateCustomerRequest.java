package io.github.carloscardoso05.api.customer.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

@Schema(description = "Request payload for creating a new customer")
public record CreateCustomerRequest(
        @NotBlank
        @Schema(description = "Customer name", example = "Carlos")
        String name,

        @Schema(description = "Customer initial balance. Defaults to 0.00 if not provided", example = "100.00", defaultValue = "0.00")
        BigDecimal balance
) {
    @Override
    public BigDecimal balance() {
        return balance != null ? balance : BigDecimal.ZERO;
    }
}

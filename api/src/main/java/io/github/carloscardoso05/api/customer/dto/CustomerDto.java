package io.github.carloscardoso05.api.customer.dto;

import io.github.carloscardoso05.api.customer.Customer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Customer response DTO")
public record CustomerDto(
        @Schema(description = "Customer ID", example = "1")
        @NotNull
        Integer id,

        @Schema(description = "Customer name", example = "Carlos")
        @NotNull
        String name,

        @Schema(description = "Customer balance", example = "100.00")
        @NotNull
        BigDecimal balance
) {
    public static CustomerDto of(Customer customer) {
        return new CustomerDto(
                customer.getId(),
                customer.getName(),
                customer.getBalance()
        );
    }
}

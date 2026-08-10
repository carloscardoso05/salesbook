package io.github.carloscardoso05.api.customer.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Request payload for updating an existing customer. Only non-null fields are applied.")
public record UpdateCustomerRequest(
        @Schema(description = "New customer name", example = "Carlos")
        String name,

        @Schema(description = "New customer balance", example = "200.00")
        BigDecimal balance
) {
}

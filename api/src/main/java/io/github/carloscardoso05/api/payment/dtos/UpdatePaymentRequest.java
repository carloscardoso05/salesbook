package io.github.carloscardoso05.api.payment.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Request payload for updating an existing payment. Only non-null fields are applied.")
public record UpdatePaymentRequest(
        @Positive
        @Schema(description = "New payment value", example = "200.00")
        BigDecimal value,

        @Schema(description = "New payment date and time")
        Instant paidAt
) {
}

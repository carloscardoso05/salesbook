package io.github.carloscardoso05.api.payment.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Request payload for creating a new payment")
public record CreatePaymentRequest(
        @NotNull
        @Schema(description = "ID of the customer making the payment", example = "1")
        Integer customerId,

        @NotNull
        @Positive
        @Schema(description = "Payment value", example = "150.00")
        BigDecimal value,

        @Schema(description = "Payment date and time. Defaults to now if not provided")
        Instant paidAt
) {
    @Override
    public Instant paidAt() {
        return paidAt != null ? paidAt : Instant.now();
    }
}

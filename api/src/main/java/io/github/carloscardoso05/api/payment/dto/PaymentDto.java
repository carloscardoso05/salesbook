package io.github.carloscardoso05.api.payment.dto;

import io.github.carloscardoso05.api.payment.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Payment response DTO")
public record PaymentDto(
        @Schema(description = "Payment ID", example = "1")
        @NotNull
        Integer id,

        @Schema(description = "Payment value", example = "150.00")
        @NotNull
        BigDecimal value,

        @Schema(description = "Customer ID", example = "1")
        @NotNull
        Integer customerId,

        @Schema(description = "Customer name", example = "Carlos")
        @NotNull
        String customerName,

        @Schema(description = "Payment date and time")
        @NotNull
        Instant paidAt
) {
    public static PaymentDto of(Payment payment) {
        return new PaymentDto(
                payment.getId(),
                payment.getValue(),
                payment.getCustomer().getId(),
                payment.getCustomer().getName(),
                payment.getPaidAt()
        );
    }
}

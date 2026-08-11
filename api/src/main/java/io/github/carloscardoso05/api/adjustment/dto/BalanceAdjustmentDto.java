package io.github.carloscardoso05.api.adjustment.dto;

import io.github.carloscardoso05.api.adjustment.BalanceAdjustment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Balance adjustment response DTO")
public record BalanceAdjustmentDto(
        @Schema(description = "Balance adjustment ID", example = "1")
        Integer id,

        @Schema(description = "Customer ID", example = "1")
        Integer customerId,

        @Schema(description = "Customer name", example = "Carlos")
        String customerName,

        @Schema(description = "Amount added to the customer balance. Negative values decrease it", example = "-50.00")
        BigDecimal value,

        @Schema(description = "Adjustment date and time")
        Instant adjustedAt
) {
    public static BalanceAdjustmentDto of(BalanceAdjustment adjustment) {
        return new BalanceAdjustmentDto(
                adjustment.getId(),
                adjustment.getCustomer().getId(),
                adjustment.getCustomer().getName(),
                adjustment.getValue(),
                adjustment.getAdjustedAt()
        );
    }
}

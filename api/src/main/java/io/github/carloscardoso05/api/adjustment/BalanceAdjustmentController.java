package io.github.carloscardoso05.api.adjustment;

import io.github.carloscardoso05.api.adjustment.dto.BalanceAdjustmentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/adjustments")
@RequiredArgsConstructor
@Tag(name = "Adjustments", description = "Read-only balance adjustment operations")
public class BalanceAdjustmentController {
    private final BalanceAdjustmentService balanceAdjustmentService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List balance adjustments", description = "Returns a paginated list of all balance adjustments, or filtered by a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of balance adjustments"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public Page<BalanceAdjustmentDto> listAdjustments(
            @Parameter(description = "Customer ID to filter by", example = "1")
            @RequestParam(value = "customerId", required = false) Integer customerId,
            @ParameterObject Pageable pageable) {
        return balanceAdjustmentService.listAdjustments(pageable, customerId);
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Find balance adjustment by ID", description = "Returns a single balance adjustment by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance adjustment found"),
            @ApiResponse(responseCode = "404", description = "Balance adjustment not found")
    })
    public BalanceAdjustmentDto findAdjustmentById(
            @Parameter(description = "Balance adjustment ID", example = "1") @PathVariable Integer id) {
        return balanceAdjustmentService.findAdjustmentById(id);
    }
}

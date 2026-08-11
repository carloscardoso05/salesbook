package io.github.carloscardoso05.api.orderitem;

import io.github.carloscardoso05.api.orderitem.dto.CreateOrderItemRequest;
import io.github.carloscardoso05.api.orderitem.dto.OrderItemDto;
import io.github.carloscardoso05.api.orderitem.dto.UpdateOrderItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/orders/{orderId}/items")
@RequiredArgsConstructor
@Tag(name = "OrderItems", description = "Order item operations")
public class OrderItemController {
    private final OrderItemService orderItemService;

    @GetMapping
    @Operation(summary = "List order items", description = "Returns all items of an order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of order items"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public List<OrderItemDto> listItems(
            @Parameter(description = "Order ID", example = "1") @PathVariable Integer orderId) {
        return orderItemService.listItems(orderId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create order item", description = "Adds an item to an order, deducting its price from the customer balance and decrementing the product stock")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order item created"),
            @ApiResponse(responseCode = "400", description = "Validation error or insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Order or product not found")
    })
    public ResponseEntity<OrderItemDto> createItem(
            @Parameter(description = "Order ID", example = "1") @PathVariable Integer orderId,
            @RequestBody @Valid CreateOrderItemRequest request) {
        var dto = orderItemService.createItem(orderId, request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{itemId}")
                .buildAndExpand(dto.id())
                .toUri();
        return ResponseEntity.created(location).body(dto);
    }

    @PutMapping(path = "/{itemId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update order item", description = "Updates an existing order item. Only non-null fields are applied.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order item updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Order item not found")
    })
    public OrderItemDto updateItem(
            @Parameter(description = "Order ID", example = "1") @PathVariable Integer orderId,
            @Parameter(description = "Order item ID", example = "1") @PathVariable Integer itemId,
            @RequestBody @Valid UpdateOrderItemRequest request) {
        return orderItemService.updateItem(orderId, itemId, request);
    }

    @DeleteMapping(path = "/{itemId}")
    @Operation(summary = "Delete order item", description = "Deletes an item from an order, refunding its price to the customer balance and incrementing the product stock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order item deleted"),
            @ApiResponse(responseCode = "404", description = "Order item not found")
    })
    public void deleteItem(
            @Parameter(description = "Order ID", example = "1") @PathVariable Integer orderId,
            @Parameter(description = "Order item ID", example = "1") @PathVariable Integer itemId) {
        orderItemService.deleteItem(orderId, itemId);
    }
}

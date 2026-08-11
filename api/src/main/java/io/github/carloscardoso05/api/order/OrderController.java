package io.github.carloscardoso05.api.order;

import io.github.carloscardoso05.api.order.dto.CreateOrderRequest;
import io.github.carloscardoso05.api.order.dto.OrderDto;
import io.github.carloscardoso05.api.order.dto.UpdateOrderRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order CRUD operations")
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List orders", description = "Returns a paginated list of all orders, or filtered by a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of orders")
    })
    public Page<OrderDto> listOrders(
            @Parameter(description = "Customer ID to filter by", example = "1")
            @RequestParam(value = "customerId", required = false) Integer customerId,
            @ParameterObject Pageable pageable) {
        return orderService.listOrders(pageable, customerId);
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Find order by ID", description = "Returns a single order by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public OrderDto findOrderById(@Parameter(description = "Order ID", example = "1") @PathVariable Integer id) {
        return orderService.findOrderById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create order", description = "Creates a new order for a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<OrderDto> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        var dto = orderService.createOrder(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(dto.id())
                .toUri();
        return ResponseEntity.created(location).body(dto);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update order", description = "Updates an existing order. Only non-null fields are applied.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public OrderDto updateOrder(
            @Parameter(description = "Order ID", example = "1") @PathVariable Integer id,
            @RequestBody @Valid UpdateOrderRequest request) {
        return orderService.updateOrder(id, request);
    }

    @DeleteMapping(path = "/{id}")
    @Operation(summary = "Delete order", description = "Deletes an order by its ID, refunding the customer balance and restocking the products")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order deleted"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public void deleteOrder(
            @Parameter(description = "Order ID", example = "1") @PathVariable Integer id) {
        orderService.deleteOrder(id);
    }
}

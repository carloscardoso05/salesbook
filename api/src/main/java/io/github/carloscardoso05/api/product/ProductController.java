package io.github.carloscardoso05.api.product;

import io.github.carloscardoso05.api.product.dto.CreateProductRequest;
import io.github.carloscardoso05.api.product.dto.ProductDto;
import io.github.carloscardoso05.api.product.dto.UpdateProductRequest;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product CRUD operations")
public class ProductController {
    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List products", description = "Returns a paginated list of all products")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of products")
    })
    public Page<ProductDto> listProducts(@ParameterObject Pageable pageable) {
        return productService.listProducts(pageable);
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Find product by ID", description = "Returns a single product by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ProductDto findProductById(@Parameter(description = "Product ID", example = "1") @PathVariable Integer id) {
        return productService.findProductById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create product", description = "Creates a new product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Product name already exists")
    })
    public ResponseEntity<ProductDto> createProduct(@RequestBody @Valid CreateProductRequest request) {
        var dto = productService.createProduct(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(dto.id())
                .toUri();
        return ResponseEntity.created(location).body(dto);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update product", description = "Updates an existing product. Only non-null fields are applied.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Product name already exists or concurrent modification")
    })
    public ProductDto updateProduct(
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id,
            @RequestBody @Valid UpdateProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping(path = "/{id}")
    @Operation(summary = "Delete product", description = "Deletes a product by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product deleted"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Product is referenced by order items")
    })
    public void deleteProduct(
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id) {
        productService.deleteProduct(id);
    }
}

package io.github.carloscardoso05.api.product;

import io.github.carloscardoso05.api.product.dto.CreateProductRequest;
import io.github.carloscardoso05.api.product.dto.ProductDto;
import io.github.carloscardoso05.api.product.dto.UpdateProductRequest;
import io.github.carloscardoso05.api.shared.DuplicateException;
import io.github.carloscardoso05.api.shared.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product("Coffee", 10);
    }

    @Test
    void listProducts() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(product), pageable, 1);
        when(productRepository.findAll(pageable)).thenReturn(page);

        Page<ProductDto> result = productService.listProducts(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Coffee");
    }

    @Test
    void findProductById() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        var result = productService.findProductById(1);

        assertThat(result.name()).isEqualTo("Coffee");
        assertThat(result.stock()).isEqualTo(10);
    }

    @Test
    void findProductById_notFound() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findProductById(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product for id 99 not found");
    }

    @Test
    void createProduct() {
        var request = new CreateProductRequest("Tea", 5);
        when(productRepository.existsByNameIgnoreCase("Tea")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0, Product.class));

        var result = productService.createProduct(request);

        assertThat(result.name()).isEqualTo("Tea");
        assertThat(result.stock()).isEqualTo(5);
    }

    @Test
    void createProduct_defaultStock() {
        var request = new CreateProductRequest("Tea", null);
        when(productRepository.existsByNameIgnoreCase("Tea")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0, Product.class));

        var result = productService.createProduct(request);

        assertThat(result.stock()).isZero();
    }

    @Test
    void createProduct_existingName() {
        var request = new CreateProductRequest("Coffee", 5);
        when(productRepository.existsByNameIgnoreCase("Coffee")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("Product with name 'Coffee' already exists.");
    }

    @Test
    void updateProduct() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        var request = new UpdateProductRequest(null, 15);
        var result = productService.updateProduct(1, request);

        assertThat(result.stock()).isEqualTo(15);
        assertThat(product.getStock()).isEqualTo(15);
    }

    @Test
    void updateProduct_idNotFound() {
        when(productRepository.existsByNameIgnoreCaseAndIdNot("Anyone", 99)).thenReturn(false);
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        var request = new UpdateProductRequest("Anyone", null);

        assertThatThrownBy(() -> productService.updateProduct(99, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product for id 99 not found");
    }

    @Test
    void updateProduct_existingName() {
        var request = new UpdateProductRequest("Tea", null);
        when(productRepository.existsByNameIgnoreCaseAndIdNot("Tea", 1)).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(1, request))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("Product with name 'Tea' already exists.");
    }

    @Test
    void deleteProduct() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        productService.deleteProduct(1);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_notFound() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product for id 99 not found");
    }
}

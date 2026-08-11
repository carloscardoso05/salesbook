package io.github.carloscardoso05.api.product;

import io.github.carloscardoso05.api.product.dto.CreateProductRequest;
import io.github.carloscardoso05.api.product.dto.ProductDto;
import io.github.carloscardoso05.api.product.dto.UpdateProductRequest;
import io.github.carloscardoso05.api.shared.DuplicateException;
import io.github.carloscardoso05.api.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductDto> listProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductDto::of);
    }

    @Transactional(readOnly = true)
    public ProductDto findProductById(Integer id) {
        return ProductDto.of(getProductById(id));
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        if (productRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateException("Product with name '%s' already exists.".formatted(request.name()));
        }
        var product = new Product(request.name(), request.stock());
        return ProductDto.of(productRepository.save(product));
    }

    @Transactional
    public ProductDto updateProduct(Integer id, UpdateProductRequest request) {
        if (productRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateException("Product with name '%s' already exists.".formatted(request.name()));
        }
        var product = getProductById(id);
        if (StringUtils.hasText(request.name())) {
            product.setName(request.name());
        }
        if (request.stock() != null) {
            product.setStock(request.stock());
        }
        return ProductDto.of(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Integer id) {
        productRepository.delete(getProductById(id));
    }

    private Product getProductById(Integer id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException(Product.class, id));
    }
}

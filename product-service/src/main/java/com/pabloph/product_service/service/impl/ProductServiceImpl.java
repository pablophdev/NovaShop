package com.pabloph.product_service.service.impl;

import com.pabloph.product_service.dto.CreateProductRequest;
import com.pabloph.product_service.dto.ProductResponse;
import com.pabloph.product_service.dto.UpdateProductRequest;
import com.pabloph.product_service.dto.UpdateStockRequest;
import com.pabloph.product_service.entity.Category;
import com.pabloph.product_service.entity.Product;
import com.pabloph.product_service.repository.CategoryRepository;
import com.pabloph.product_service.repository.ProductRepository;
import com.pabloph.product_service.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@Validated
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public ProductResponse createProduct(CreateProductRequest request) {
        validateSkuDoesNotExist(request.sku());
        Category category = getCategory(request.categoryId());
        LocalDateTime now = LocalDateTime.now();

        Product product = new Product();
        product.setName(request.name());
        product.setSku(request.sku());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setActive(request.active());
        product.setCategory(category.getName());
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return toResponse(getProduct(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product sku is required");
        }

        return productRepository.findBySku(sku)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is required");
        }

        return productRepository.findByNameContainingIgnoreCase(name, pageable).map(this::toResponse);
    }

    @Override
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = getProduct(id);
        Category category = getCategory(request.categoryId());

        if (!product.getSku().equals(request.sku())) {
            validateSkuDoesNotExist(request.sku());
        }

        product.setName(request.name());
        product.setSku(request.sku());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setActive(request.active());
        product.setCategory(category.getName());
        product.setUpdatedAt(LocalDateTime.now());

        return toResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse updateStock(Long id, UpdateStockRequest request) {
        Product product = getProduct(id);
        product.setStock(request.stock());
        product.setUpdatedAt(LocalDateTime.now());

        return toResponse(productRepository.save(product));
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = getProduct(id);
        productRepository.delete(product);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private void validateSkuDoesNotExist(String sku) {
        if (productRepository.existsBySku(sku)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product sku already exists");
        }
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getActive(),
                product.getCategory(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

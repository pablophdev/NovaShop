package com.pabloph.product_service.service;

import com.pabloph.product_service.dto.CreateProductRequest;
import com.pabloph.product_service.dto.ProductResponse;
import com.pabloph.product_service.dto.UpdateProductRequest;
import com.pabloph.product_service.dto.UpdateStockRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse createProduct(@Valid CreateProductRequest request);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySku(String sku);

    Page<ProductResponse> getProducts(Pageable pageable);

    Page<ProductResponse> searchProducts(String name, Pageable pageable);

    ProductResponse updateProduct(Long id, @Valid UpdateProductRequest request);

    ProductResponse updateStock(Long id, @Valid UpdateStockRequest request);

    void deleteProduct(Long id);
}

package com.pabloph.product_service.service;

import com.pabloph.product_service.dto.CreateProductRequest;
import com.pabloph.product_service.dto.ProductResponse;
import com.pabloph.product_service.dto.UpdateProductRequest;
import com.pabloph.product_service.dto.UpdateStockRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySku(String sku);

    Page<ProductResponse> getProducts(Pageable pageable);

    Page<ProductResponse> searchProducts(String name, Pageable pageable);

    ProductResponse updateProduct(Long id, UpdateProductRequest request);

    ProductResponse updateStock(Long id, UpdateStockRequest request);

    void deleteProduct(Long id);
}

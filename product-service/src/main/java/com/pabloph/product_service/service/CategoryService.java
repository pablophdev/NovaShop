package com.pabloph.product_service.service;

import com.pabloph.product_service.dto.CategoryRequest;
import com.pabloph.product_service.dto.CategoryResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    CategoryResponse createCategory(@Valid CategoryRequest request);

    CategoryResponse getCategoryById(Long id);

    Page<CategoryResponse> getCategories(Pageable pageable);

    Page<CategoryResponse> searchCategories(String name, Pageable pageable);

    CategoryResponse updateCategory(Long id, @Valid CategoryRequest request);

    void deleteCategory(Long id);
}

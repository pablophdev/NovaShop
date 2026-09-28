package com.pabloph.product_service.service;

import com.pabloph.product_service.dto.CategoryRequest;
import com.pabloph.product_service.dto.CategoryResponse;
import com.pabloph.product_service.entity.Category;
import com.pabloph.product_service.repository.CategoryRepository;
import com.pabloph.product_service.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository);
    }

    @Test
    void createCategorySavesCategoryWhenNameIsAvailable() {
        CategoryRequest request = new CategoryRequest("Electronics", "Electronic devices");
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(1L);
            return category;
        });

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Electronics");
        assertThat(response.description()).isEqualTo("Electronic devices");
    }

    @Test
    void createCategoryThrowsBadRequestWhenNameAlreadyExists() {
        CategoryRequest request = new CategoryRequest("Electronics", "Electronic devices");
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategoryKeepsSameNameWithoutDuplicateValidation() {
        Category category = category(1L, "Electronics", "Old description");
        CategoryRequest request = new CategoryRequest("electronics", "New description");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.updateCategory(1L, request);

        assertThat(response.name()).isEqualTo("electronics");
        assertThat(response.description()).isEqualTo("New description");
        verify(categoryRepository, never()).existsByNameIgnoreCase(any());
    }

    @Test
    void searchCategoriesThrowsBadRequestWhenNameIsBlank() {
        assertThatThrownBy(() -> categoryService.searchCategories(" ", PageRequest.of(0, 10)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(categoryRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void getCategoriesReturnsMappedPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(categoryRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(category(1L, "Electronics", "Electronic devices")), pageable, 1));

        Page<CategoryResponse> response = categoryService.getCategories(pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().getFirst().name()).isEqualTo("Electronics");
    }

    private static Category category(Long id, String name, String description) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription(description);
        return category;
    }
}

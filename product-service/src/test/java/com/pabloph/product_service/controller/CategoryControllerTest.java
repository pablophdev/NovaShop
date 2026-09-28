package com.pabloph.product_service.controller;

import com.pabloph.product_service.dto.CategoryRequest;
import com.pabloph.product_service.dto.CategoryResponse;
import com.pabloph.product_service.exception.GlobalExceptionHandler;
import com.pabloph.product_service.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryController(categoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void createCategoryReturnsCreatedCategory() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(categoryResponse());

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Electronics",
                                  "description": "Electronic devices"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"));

        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    void createCategoryReturnsBadRequestWhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "description": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors", hasKey("name")))
                .andExpect(jsonPath("$.validationErrors", hasKey("description")));

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    void getCategoriesReturnsPage() throws Exception {
        when(categoryService.getCategories(any()))
                .thenReturn(new PageImpl<>(List.of(categoryResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Electronics"));
    }

    @Test
    void getCategoryByIdReturnsNotFoundWhenServiceThrowsNotFound() throws Exception {
        when(categoryService.getCategoryById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        mockMvc.perform(get("/api/categories/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"))
                .andExpect(jsonPath("$.path").value("/api/categories/99"));
    }

    private static CategoryResponse categoryResponse() {
        return new CategoryResponse(1L, "Electronics", "Electronic devices");
    }
}

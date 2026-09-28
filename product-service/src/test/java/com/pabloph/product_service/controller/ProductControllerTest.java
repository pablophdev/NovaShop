package com.pabloph.product_service.controller;

import com.pabloph.product_service.dto.CreateProductRequest;
import com.pabloph.product_service.dto.ProductResponse;
import com.pabloph.product_service.dto.UpdateStockRequest;
import com.pabloph.product_service.exception.GlobalExceptionHandler;
import com.pabloph.product_service.service.ProductService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void createProductReturnsCreatedProduct() throws Exception {
        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(productResponse());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Keyboard",
                                  "sku": "KEY-001",
                                  "description": "Mechanical keyboard",
                                  "price": 99.99,
                                  "stock": 12,
                                  "active": true,
                                  "categoryId": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.sku").value("KEY-001"));

        verify(productService).createProduct(any(CreateProductRequest.class));
    }

    @Test
    void createProductReturnsBadRequestWhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "sku": "",
                                  "description": "",
                                  "price": 0,
                                  "stock": -1,
                                  "active": null,
                                  "categoryId": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors", hasKey("name")))
                .andExpect(jsonPath("$.validationErrors", hasKey("sku")));

        verify(productService, never()).createProduct(any());
    }

    @Test
    void getProductsReturnsPage() throws Exception {
        when(productService.getProducts(any()))
                .thenReturn(new PageImpl<>(List.of(productResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("KEY-001"));
    }

    @Test
    void getProductByIdReturnsNotFoundWhenServiceThrowsNotFound() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        mockMvc.perform(get("/api/products/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"))
                .andExpect(jsonPath("$.path").value("/api/products/99"));
    }

    @Test
    void updateStockReturnsUpdatedProduct() throws Exception {
        ProductResponse response = new ProductResponse(
                1L,
                "Keyboard",
                "KEY-001",
                "Mechanical keyboard",
                BigDecimal.valueOf(99.99),
                5,
                true,
                "Electronics",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(productService.updateStock(eq(1L), any(UpdateStockRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/products/{id}/stock", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stock": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
    }

    private static ProductResponse productResponse() {
        return new ProductResponse(
                1L,
                "Keyboard",
                "KEY-001",
                "Mechanical keyboard",
                BigDecimal.valueOf(99.99),
                12,
                true,
                "Electronics",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}

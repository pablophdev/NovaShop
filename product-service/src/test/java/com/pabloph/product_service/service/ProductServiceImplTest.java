package com.pabloph.product_service.service;

import com.pabloph.product_service.dto.CreateProductRequest;
import com.pabloph.product_service.dto.ProductResponse;
import com.pabloph.product_service.dto.UpdateStockRequest;
import com.pabloph.product_service.entity.Category;
import com.pabloph.product_service.entity.Product;
import com.pabloph.product_service.repository.CategoryRepository;
import com.pabloph.product_service.repository.ProductRepository;
import com.pabloph.product_service.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, categoryRepository);
    }

    @Test
    void createProductSavesProductWithCategoryName() {
        CreateProductRequest request = new CreateProductRequest(
                "Keyboard",
                "KEY-001",
                "Mechanical keyboard",
                BigDecimal.valueOf(99.99),
                12,
                true,
                10L
        );
        Category category = category(10L, "Electronics", "Electronic devices");

        when(productRepository.existsBySku("KEY-001")).thenReturn(false);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(1L);
            return product;
        });

        ProductResponse response = productService.createProduct(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Keyboard");
        assertThat(response.sku()).isEqualTo("KEY-001");
        assertThat(response.category()).isEqualTo("Electronics");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getCreatedAt()).isNotNull();
        assertThat(productCaptor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void createProductThrowsBadRequestWhenSkuAlreadyExists() {
        CreateProductRequest request = new CreateProductRequest(
                "Keyboard",
                "KEY-001",
                "Mechanical keyboard",
                BigDecimal.valueOf(99.99),
                12,
                true,
                10L
        );

        when(productRepository.existsBySku("KEY-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(categoryRepository, never()).findById(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateStockUpdatesExistingProduct() {
        Product product = product(1L, "Keyboard", "KEY-001", 12);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.updateStock(1L, new UpdateStockRequest(7));

        assertThat(response.stock()).isEqualTo(7);
        assertThat(product.getStock()).isEqualTo(7);
        assertThat(product.getUpdatedAt()).isNotNull();
    }

    @Test
    void searchProductsThrowsBadRequestWhenNameIsBlank() {
        assertThatThrownBy(() -> productService.searchProducts(" ", PageRequest.of(0, 10)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(productRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void getProductsReturnsMappedPage() {
        Product product = product(1L, "Keyboard", "KEY-001", 12);
        PageRequest pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        Page<ProductResponse> response = productService.getProducts(pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().getFirst().sku()).isEqualTo("KEY-001");
    }

    private static Category category(Long id, String name, String description) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription(description);
        return category;
    }

    private static Product product(Long id, String name, String sku, Integer stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSku(sku);
        product.setDescription("Description");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setStock(stock);
        product.setActive(true);
        product.setCategory("Electronics");
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }
}

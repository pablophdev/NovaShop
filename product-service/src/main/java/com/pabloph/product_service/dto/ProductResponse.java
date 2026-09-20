package com.pabloph.product_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        String description,
        BigDecimal price,
        Integer stock,
        Boolean active,
        String category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

package com.pabloph.product_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStockRequest(
        @NotNull
        @PositiveOrZero
        Integer stock
) {
}

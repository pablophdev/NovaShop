package com.pabloph.order_service.dto.client;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStockRequest(
        @NotNull
        @PositiveOrZero
        Integer stock
) {
}

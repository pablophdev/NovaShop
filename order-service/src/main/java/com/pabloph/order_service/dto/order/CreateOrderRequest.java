package com.pabloph.order_service.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "Customer id is required")
        Long customerId,

        @Valid
        @NotEmpty(message = "Order items are required")
        List<CreateOrderItemRequest> items
) {
}

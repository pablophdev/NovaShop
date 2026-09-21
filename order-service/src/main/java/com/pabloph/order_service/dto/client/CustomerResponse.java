package com.pabloph.order_service.dto.client;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        Boolean active,
        LocalDateTime createdAt
) {
}

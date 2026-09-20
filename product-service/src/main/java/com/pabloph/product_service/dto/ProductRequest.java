package com.pabloph.product_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(
    @NotBlank
    @Size(max = 100)
    String name,

    @NotBlank
    @Size(max = 1000)
    String description,

    @NotNull
    @Positive
    BigDecimal price,

    @NotNull
    @PositiveOrZero
    Integer stock,

    @NotNull
    Boolean active,

    @NotNull
    Long categoryId
)
 {
}

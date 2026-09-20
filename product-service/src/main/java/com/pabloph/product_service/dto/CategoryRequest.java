package com.pabloph.product_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank
    @Size(max = 100)
    String name,

    @NotBlank
    @Size(max = 1000)
    String description
)
 {
}

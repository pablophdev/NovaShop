package com.pabloph.ms_auth.dto;

import com.pabloph.ms_auth.entity.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        Role role,
        Boolean active,
        LocalDateTime createdAt
) {
}

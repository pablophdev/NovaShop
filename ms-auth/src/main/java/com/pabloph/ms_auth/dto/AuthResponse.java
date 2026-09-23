package com.pabloph.ms_auth.dto;

public record AuthResponse(
        String token,
        String type,
        Long expiresIn,
        UserResponse user
) {
}

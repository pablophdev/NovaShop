package com.pabloph.ms_auth.service;

import com.pabloph.ms_auth.entity.Role;
import com.pabloph.ms_auth.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters";

    @Test
    void generateTokenIncludesEmailAndRole() {
        JwtService jwtService = new JwtService(SECRET, 3600000L);

        String token = jwtService.generateToken(user(Role.ADMIN));

        assertThat(jwtService.extractEmail(token)).isEqualTo("admin@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.getExpiration()).isEqualTo(3600000L);
    }

    @Test
    void isTokenValidThrowsWhenTokenIsExpired() {
        JwtService jwtService = new JwtService(SECRET, -1000L);
        String token = jwtService.generateToken(user(Role.USER));

        assertThatThrownBy(() -> jwtService.isTokenValid(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    private static User user(Role role) {
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPassword("encoded-password");
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}

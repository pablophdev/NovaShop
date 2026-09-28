package com.pabloph.api_gateway.config;

import com.pabloph.api_gateway.exception.GatewayErrorResponseWriter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringJUnitConfig(SecurityConfigTest.TestConfig.class)
class SecurityConfigTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters";

    private final ApplicationContext applicationContext;
    private WebTestClient webTestClient;

    SecurityConfigTest(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void publicProductReadIsAllowedWithoutToken() {
        webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("products");
    }

    @Test
    void healthIsAllowedWithoutToken() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("health");
    }

    @Test
    void adminProductMutationRequiresAuthentication() {
        webTestClient.post()
                .uri("/api/products")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Authentication required")
                .jsonPath("$.path").isEqualTo("/api/products");
    }

    @Test
    void adminProductMutationRejectsUserRole() {
        webTestClient.post()
                .uri("/api/products")
                .header("Authorization", "Bearer " + token("user@example.com", "USER"))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Access denied")
                .jsonPath("$.path").isEqualTo("/api/products");
    }

    @Test
    void adminProductMutationAllowsAdminRole() {
        webTestClient.post()
                .uri("/api/products")
                .header("Authorization", "Bearer " + token("admin@example.com", "ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("product-created");
    }

    @Test
    void authenticatedCustomerResourceAllowsUserRole() {
        webTestClient.get()
                .uri("/api/customers/1")
                .header("Authorization", "Bearer " + token("user@example.com", "USER"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("customer-1");
    }

    @Test
    void customerListRequiresAdminRole() {
        webTestClient.get()
                .uri("/api/customers")
                .header("Authorization", "Bearer " + token("user@example.com", "USER"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void unknownRouteIsDenied() {
        webTestClient.get()
                .uri("/api/unknown")
                .header("Authorization", "Bearer " + token("admin@example.com", "ADMIN"))
                .exchange()
                .expectStatus().isForbidden();
    }

    private static String token(String email, String role) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + 3600000L);
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(signingKey())
                .compact();
    }

    private static SecretKey signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Configuration
    @EnableWebFlux
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        JwtAuthenticationWebFilter jwtAuthenticationWebFilter() {
            return new JwtAuthenticationWebFilter(SECRET);
        }

        @Bean
        GatewayErrorResponseWriter gatewayErrorResponseWriter() {
            return new GatewayErrorResponseWriter();
        }

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/actuator/health")
        Mono<String> health() {
            return Mono.just("health");
        }

        @GetMapping("/api/products")
        Mono<String> products() {
            return Mono.just("products");
        }

        @PostMapping("/api/products")
        Mono<String> createProduct() {
            return Mono.just("product-created");
        }

        @PatchMapping("/api/products/{id}/stock")
        Mono<String> updateProductStock(@PathVariable Long id) {
            return Mono.just("product-stock-" + id);
        }

        @GetMapping("/api/customers")
        Mono<String> customers() {
            return Mono.just("customers");
        }

        @GetMapping("/api/customers/{id}")
        Mono<String> customer(@PathVariable Long id) {
            return Mono.just("customer-" + id);
        }

        @GetMapping("/api/unknown")
        Mono<String> unknown() {
            return Mono.just("unknown");
        }

        @RequestMapping("/api/auth/me")
        Mono<String> me() {
            return Mono.just("me");
        }
    }
}

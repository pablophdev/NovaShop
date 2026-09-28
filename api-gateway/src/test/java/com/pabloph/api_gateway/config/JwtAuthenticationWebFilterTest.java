package com.pabloph.api_gateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationWebFilterTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters";

    private final JwtAuthenticationWebFilter filter = new JwtAuthenticationWebFilter(SECRET);

    @Test
    void filterAddsAuthenticationToReactiveContextWhenBearerTokenIsValid() {
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .header("Authorization", "Bearer " + token("admin@example.com", "ADMIN", 3600000L))
        );
        WebFilterChain chain = webExchange -> ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .doOnNext(authentication::set)
                .then();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(authentication.get()).isNotNull();
        assertThat(authentication.get().getName()).isEqualTo("admin@example.com");
        assertThat(authentication.get().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void filterDoesNotAuthenticateWhenAuthorizationHeaderIsMissing() {
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products"));
        WebFilterChain chain = webExchange -> ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .doOnNext(authentication::set)
                .then();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(authentication.get()).isNull();
    }

    @Test
    void filterDoesNotAuthenticateWhenBearerTokenIsExpired() {
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .header("Authorization", "Bearer " + token("user@example.com", "USER", -1000L))
        );
        WebFilterChain chain = webExchange -> ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .doOnNext(authentication::set)
                .then();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(authentication.get()).isNull();
    }

    @Test
    void filterDoesNotAuthenticateWhenBearerTokenIsInvalid() {
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .header("Authorization", "Bearer invalid-token")
        );
        WebFilterChain chain = webExchange -> ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .doOnNext(authentication::set)
                .then();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(authentication.get()).isNull();
    }

    private static String token(String email, String role, long expirationMillis) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMillis);
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
}

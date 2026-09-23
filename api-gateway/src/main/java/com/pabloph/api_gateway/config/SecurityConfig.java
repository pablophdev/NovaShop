package com.pabloph.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JwtAuthenticationWebFilter jwtAuthenticationWebFilter) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
				.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
				.authorizeExchange(exchanges -> exchanges
						.pathMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
						.pathMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						.pathMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
						.pathMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
						.anyExchange().authenticated())
				.addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
				.build();
	}
}

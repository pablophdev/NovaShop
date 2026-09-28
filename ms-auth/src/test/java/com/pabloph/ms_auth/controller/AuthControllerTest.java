package com.pabloph.ms_auth.controller;

import com.pabloph.ms_auth.dto.AuthResponse;
import com.pabloph.ms_auth.dto.LoginRequest;
import com.pabloph.ms_auth.dto.RegisterRequest;
import com.pabloph.ms_auth.dto.UserResponse;
import com.pabloph.ms_auth.entity.Role;
import com.pabloph.ms_auth.exception.GlobalExceptionHandler;
import com.pabloph.ms_auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registerReturnsCreatedAuthResponse() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "pedro@example.com",
                                  "password": "secret12",
                                  "firstName": "Pedro",
                                  "lastName": "Perez"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("pedro@example.com"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void registerReturnsBadRequestWhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "123",
                                  "firstName": "",
                                  "lastName": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors", hasKey("email")))
                .andExpect(jsonPath("$.validationErrors", hasKey("password")))
                .andExpect(jsonPath("$.validationErrors", hasKey("firstName")))
                .andExpect(jsonPath("$.validationErrors", hasKey("lastName")));

        verify(authService, never()).register(any());
    }

    @Test
    void loginReturnsAuthResponse() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "pedro@example.com",
                                  "password": "secret12"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.role").value("USER"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void loginReturnsUnauthorizedWhenServiceThrowsUnauthorized() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "pedro@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void getCurrentUserReturnsUserResponse() throws Exception {
        when(authService.getCurrentUser()).thenReturn(userResponse());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("pedro@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    private static AuthResponse authResponse() {
        return new AuthResponse("jwt-token", "Bearer", 3600000L, userResponse());
    }

    private static UserResponse userResponse() {
        return new UserResponse(
                1L,
                "pedro@example.com",
                "Pedro",
                "Perez",
                Role.USER,
                true,
                LocalDateTime.now()
        );
    }
}

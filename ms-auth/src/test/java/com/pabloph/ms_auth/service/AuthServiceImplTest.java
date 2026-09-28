package com.pabloph.ms_auth.service;

import com.pabloph.ms_auth.dto.AuthResponse;
import com.pabloph.ms_auth.dto.LoginRequest;
import com.pabloph.ms_auth.dto.RegisterRequest;
import com.pabloph.ms_auth.dto.UserResponse;
import com.pabloph.ms_auth.entity.Role;
import com.pabloph.ms_auth.entity.User;
import com.pabloph.ms_auth.repository.UserRepository;
import com.pabloph.ms_auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerCreatesUserWithEncodedPasswordAndUserRole() {
        RegisterRequest request = new RegisterRequest("pedro@example.com", "secret12", "Pedro", "Perez");
        when(userRepository.existsByEmail("pedro@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret12")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpiration()).thenReturn(3600000L);

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600000L);
        assertThat(response.user().email()).isEqualTo("pedro@example.com");
        assertThat(response.user().role()).isEqualTo(Role.USER);
        assertThat(response.user().active()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void registerThrowsBadRequestWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("pedro@example.com", "secret12", "Pedro", "Perez");
        when(userRepository.existsByEmail("pedro@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("pedro@example.com", "secret12");
        User user = user(1L, "pedro@example.com", "encoded-password", true, Role.USER);

        when(userRepository.findByEmail("pedro@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret12", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpiration()).thenReturn(3600000L);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("pedro@example.com");
    }

    @Test
    void loginThrowsUnauthorizedWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest("pedro@example.com", "wrong-password");
        User user = user(1L, "pedro@example.com", "encoded-password", true, Role.USER);

        when(userRepository.findByEmail("pedro@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginThrowsUnauthorizedWhenUserIsInactive() {
        LoginRequest request = new LoginRequest("pedro@example.com", "secret12");
        User user = user(1L, "pedro@example.com", "encoded-password", false, Role.USER);

        when(userRepository.findByEmail("pedro@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void getCurrentUserReturnsAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "pedro@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
        when(userRepository.findByEmail("pedro@example.com"))
                .thenReturn(Optional.of(user(1L, "pedro@example.com", "encoded-password", true, Role.USER)));

        UserResponse response = authService.getCurrentUser();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("pedro@example.com");
    }

    @Test
    void getCurrentUserThrowsUnauthorizedWhenAuthenticationIsMissing() {
        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static User user(Long id, String email, String password, Boolean active, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword(password);
        user.setFirstName("Pedro");
        user.setLastName("Perez");
        user.setRole(role);
        user.setActive(active);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}

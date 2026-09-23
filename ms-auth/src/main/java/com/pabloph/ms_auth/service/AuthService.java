package com.pabloph.ms_auth.service;

import com.pabloph.ms_auth.dto.AuthResponse;
import com.pabloph.ms_auth.dto.LoginRequest;
import com.pabloph.ms_auth.dto.RegisterRequest;
import com.pabloph.ms_auth.dto.UserResponse;
import jakarta.validation.Valid;

public interface AuthService {

    AuthResponse register(@Valid RegisterRequest request);

    AuthResponse login(@Valid LoginRequest request);

    UserResponse getCurrentUser();
}

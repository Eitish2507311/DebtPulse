package com.debtpulse.auth.service;

import com.debtpulse.auth.dto.request.RegisterRequest;
import com.debtpulse.auth.dto.response.AuthResponse;

/** Authentication use-cases: login, admin-driven registration, token refresh and logout. */
public interface AuthService {

    AuthResponse login(String email, String password);

    AuthResponse register(RegisterRequest request);

    /** Exchange a valid refresh token for a rotated token pair. */
    AuthResponse refresh(String refreshToken);

    /** End the session the refresh token belongs to. */
    void logout(String refreshToken);
}

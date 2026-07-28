package com.debtpulse.auth.controller;

import com.debtpulse.auth.dto.response.AuthResponse;
import com.debtpulse.auth.exception.GlobalExceptionHandler;
import com.debtpulse.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer test using standalone MockMvc (no security context) so the mapping,
 * validation and JSON contract are exercised in isolation with a mocked service.
 */
class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, 604_800_000L))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_returns200WithToken() throws Exception {
        when(authService.login(eq("admin@dp.com"), eq("password")))
                .thenReturn(new AuthResponse("Login successful", "jwt", "refresh-jwt", 10800L, "USR-001", "ADMIN", "System Admin", "B01"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", "admin@dp.com", "password", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_setsHttpOnlyRefreshCookie_andOmitsTokenFromBody() throws Exception {
        when(authService.login(eq("admin@dp.com"), eq("password")))
                .thenReturn(new AuthResponse("Login successful", "jwt", "refresh-jwt", 10800L, "USR-001", "ADMIN", "System Admin", "B01"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", "admin@dp.com", "password", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())   // never in the body
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=refresh-jwt")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Strict")));
    }

    @Test
    void refresh_readsCookie_rotatesIt_andOmitsTokenFromBody() throws Exception {
        when(authService.refresh(eq("refresh-old")))
                .thenReturn(new AuthResponse("Token refreshed", "jwt2", "refresh-new", 10800L, "USR-001", "ADMIN", "System Admin", "B01"));

        mockMvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-old")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt2"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=refresh-new")));
    }

    @Test
    void refresh_withoutCookie_isRejected() throws Exception {
        // No refresh cookie → UnauthorizedActionException, which this service maps to 403 (its
        // established convention, same as a bad login).
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", "not-an-email", "password", "x"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns201() throws Exception {
        when(authService.register(any()))
                .thenReturn(new AuthResponse("User registered successfully", "jwt", "refresh-jwt", 10800L, "USR-010", "FIELD_OFFICER", "New", "B01"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "fullName", "New Officer", "email", "new@dp.com",
                                "password", "Secret@123", "role", "FIELD_OFFICER",
                                "phone", "9876543210"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("USR-010"));
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        // "secret123" has no uppercase and no special character -> violates the strong-password policy
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "fullName", "New Officer", "email", "new@dp.com",
                                "password", "secret123", "role", "FIELD_OFFICER",
                                "phone", "9876543210"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void register_nonCorporateEmail_returns400() throws Exception {
        // gmail.com is not an approved corporate domain -> rejected
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "fullName", "New Officer", "email", "new@gmail.com",
                                "password", "Secret@123", "role", "FIELD_OFFICER",
                                "phone", "9876543210"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void register_invalidPhone_returns400() throws Exception {
        // 9-digit phone -> violates the exactly-10-digits policy
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "fullName", "New Officer", "email", "new@dp.com",
                                "password", "Secret@123", "role", "FIELD_OFFICER",
                                "phone", "987654321"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").exists());
    }
}

package com.debtpulse.auth.controller;

import com.debtpulse.auth.dto.request.PasswordRequests.ChangePasswordRequest;
import com.debtpulse.auth.dto.request.PasswordRequests.ForgotPasswordRequest;
import com.debtpulse.auth.dto.request.PasswordRequests.ResetPasswordRequest;
import com.debtpulse.auth.service.PasswordResetService;
import com.debtpulse.common.security.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Password", description = "Forgot / reset / change password flows")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset token")
    public ResponseEntity<Map<String, String>> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(service.forgotPassword(req.email()));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a valid reset token")
    public ResponseEntity<Map<String, String>> reset(@Valid @RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(service.resetPassword(req.token(), req.newPassword()));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password for the currently authenticated user")
    public ResponseEntity<Map<String, String>> change(@Valid @RequestBody ChangePasswordRequest req) {
        return ResponseEntity.ok(
                service.changePassword(AuthContext.currentUserId(), req.currentPassword(), req.newPassword()));
    }
}

package com.debtpulse.auth.service;

import java.util.Map;

/** Self-service password flows: forgot (issue token), reset (consume token), change. */
public interface PasswordResetService {

    Map<String, String> forgotPassword(String email);

    Map<String, String> resetPassword(String token, String newPassword);

    Map<String, String> changePassword(String userId, String currentPassword, String newPassword);
}

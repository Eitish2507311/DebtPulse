package com.debtpulse.auth.dto.response;

import java.time.LocalDateTime;

/** Safe user projection (never exposes passwordHash or reset/lockout fields). */
public record UserDto(
        String userId,
        String fullName,
        String email,
        String phone,
        String role,
        String branchId,
        String status,
        LocalDateTime createdAt
) {}

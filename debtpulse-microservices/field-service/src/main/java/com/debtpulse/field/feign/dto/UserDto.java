package com.debtpulse.field.feign.dto;

import java.time.LocalDateTime;

/**
 * Local copy of auth-service's {@code UserDto} JSON (field names byte-for-byte compatible
 * per INTERNAL_CONTRACTS). Used for user lookups over Feign.
 */
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

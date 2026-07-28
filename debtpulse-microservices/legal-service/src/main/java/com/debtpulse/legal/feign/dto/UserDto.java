package com.debtpulse.legal.feign.dto;

import java.time.LocalDateTime;

/**
 * Local copy of auth-service's {@code UserDto} JSON contract (INTERNAL_CONTRACTS).
 * Field names must stay byte-for-byte compatible with the provider.
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

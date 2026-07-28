package com.debtpulse.account.feign.dto;

import java.time.LocalDateTime;

/**
 * Local copy of auth-service's {@code UserDto} JSON contract. Field names are byte-for-byte
 * compatible with INTERNAL_CONTRACTS.md so Feign deserialization matches exactly.
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

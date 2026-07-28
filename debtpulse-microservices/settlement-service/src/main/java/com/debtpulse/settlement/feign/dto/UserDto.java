package com.debtpulse.settlement.feign.dto;

import java.time.LocalDateTime;

/**
 * Local copy of auth-service's {@code UserDto} JSON contract
 * (see INTERNAL_CONTRACTS: {@code GET /api/internal/users/...}).
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

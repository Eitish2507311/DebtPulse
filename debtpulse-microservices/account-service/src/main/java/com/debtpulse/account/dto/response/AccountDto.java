package com.debtpulse.account.dto.response;

import java.math.BigDecimal;

/**
 * Canonical account projection served to other microservices over Feign
 * ({@code GET /api/internal/accounts/{id}}). Field names are byte-for-byte compatible with
 * the inter-service contract in INTERNAL_CONTRACTS.md — do not rename.
 */
public record AccountDto(
        String accountId,
        String loanRef,
        String borrowerName,
        String phone,
        String address,
        String branchId,
        BigDecimal principalAmount,
        BigDecimal totalOverdue,
        Integer dpd,
        String bucket,
        String status,
        String assignedAgentId
) {}

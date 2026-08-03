package com.debtpulse.field.feign.dto;

import java.math.BigDecimal;

/**
 * Local copy of account-service's {@code AccountDto} JSON contract
 * (see INTERNAL_CONTRACTS: {@code GET /api/internal/accounts/{id}}). Field names must stay
 * byte-for-byte compatible with the provider.
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

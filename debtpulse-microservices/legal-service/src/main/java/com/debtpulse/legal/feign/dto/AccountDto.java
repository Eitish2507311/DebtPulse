package com.debtpulse.legal.feign.dto;

import java.math.BigDecimal;

/**
 * Local copy of account-service's {@code AccountDto} JSON contract (INTERNAL_CONTRACTS).
 * Field names must stay byte-for-byte compatible with the provider.
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

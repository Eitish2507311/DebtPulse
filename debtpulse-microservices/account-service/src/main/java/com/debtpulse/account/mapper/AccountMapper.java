package com.debtpulse.account.mapper;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.account.dto.request.CreateAccountRequest;
import com.debtpulse.account.dto.response.AccountDto;
import com.debtpulse.account.entity.DelinquentAccount;
import org.springframework.stereotype.Component;

/** Converts between the {@link DelinquentAccount} entity and its request/response DTOs. */
@Component
public class AccountMapper {

    public AccountDto toDto(DelinquentAccount a) {
        if (a == null) return null;
        return new AccountDto(
                a.getAccountId(),
                a.getLoanRef(),
                a.getBorrowerName(),
                a.getPhone(),
                a.getAddress(),
                a.getBranchId(),
                a.getPrincipalAmount(),
                a.getTotalOverdue(),
                a.getDpd(),
                a.getBucket() == null ? null : a.getBucket().name(),
                a.getStatus() == null ? null : a.getStatus().name(),
                a.getAssignedAgentId()
        );
    }

    /** Build a new (unclassified, unsaved) account from an onboarding request. */
    public DelinquentAccount toEntity(CreateAccountRequest req) {
        return DelinquentAccount.builder()
                .loanRef(req.loanRef())
                .borrowerName(req.borrowerName())
                .phone(req.phone())
                .address(req.address())
                .branchId(req.branchId())
                .principalAmount(req.principalAmount())
                .totalOverdue(req.totalOverdue())
                .dpd(req.dpd())
                .daysInCurrentBucket(0)
                .status(AccountStatus.ACTIVE)
                .build();
    }
}

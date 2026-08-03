package com.debtpulse.account.service;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.DpdBucket;
import com.debtpulse.account.dto.request.CreateAccountRequest;
import com.debtpulse.account.dto.request.UpdateAccountRequest;
import com.debtpulse.account.entity.DelinquentAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/** Delinquent-account portfolio management (2.2). */
public interface AccountService {

    Page<DelinquentAccount> list(DpdBucket bucket, AccountStatus status, String agentId,
                                 Integer dpdMin, Integer dpdMax, Pageable pageable);

    DelinquentAccount getById(String id);

    /** Classify the bucket from DPD, persist, then attempt auto-allocation to an agent. */
    DelinquentAccount importAccount(DelinquentAccount account, String userId);

    /**
     * Onboard an account from a request atomically: enforce unique loan reference and the
     * secured-loan collateral rule, create the account (classify + allocate) and, when supplied,
     * its collateral asset — both in one transaction.
     */
    DelinquentAccount onboard(CreateAccountRequest request, String userId);

    DpdBucket classifyBucket(int dpd);

    DelinquentAccount update(String id, UpdateAccountRequest request);

    void delete(String id);

    DelinquentAccount assignAgent(String id, String agentId);

    DelinquentAccount updateStatus(String id, AccountStatus status);

    boolean exists(String id);

    Map<String, Object> stats();
}

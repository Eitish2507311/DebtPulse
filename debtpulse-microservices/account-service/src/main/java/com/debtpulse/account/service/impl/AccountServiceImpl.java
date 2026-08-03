package com.debtpulse.account.service.impl;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.DpdBucket;
import com.debtpulse.common.enums.VerificationStatus;
import com.debtpulse.account.dto.request.CreateAccountRequest;
import com.debtpulse.account.dto.request.UpdateAccountRequest;
import com.debtpulse.account.entity.CollateralAsset;
import com.debtpulse.account.entity.DelinquentAccount;
import com.debtpulse.account.feign.AuthClient;
import com.debtpulse.account.mapper.AccountMapper;
import com.debtpulse.account.repository.AccountSpecifications;
import com.debtpulse.account.repository.CollateralAssetRepository;
import com.debtpulse.account.repository.DelinquentAccountRepository;
import com.debtpulse.account.service.AccountService;
import com.debtpulse.account.service.AllocationService;
import com.debtpulse.account.exception.BusinessRuleException;
import com.debtpulse.account.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final DelinquentAccountRepository repo;
    private final CollateralAssetRepository collateralRepo;
    private final AllocationService allocationService;
    private final AuthClient authClient;
    private final AccountMapper mapper;

    public AccountServiceImpl(DelinquentAccountRepository repo,
                              CollateralAssetRepository collateralRepo,
                              AllocationService allocationService,
                              AuthClient authClient,
                              AccountMapper mapper) {
        this.repo = repo;
        this.collateralRepo = collateralRepo;
        this.allocationService = allocationService;
        this.authClient = authClient;
        this.mapper = mapper;
    }

    @Override
    public Page<DelinquentAccount> list(DpdBucket bucket, AccountStatus status, String agentId,
                                        Integer dpdMin, Integer dpdMax, Pageable pageable) {
        return repo.findAll(AccountSpecifications.withFilters(bucket, status, agentId, dpdMin, dpdMax), pageable);
    }

    @Override
    public DelinquentAccount getById(String id) {
        return find(id);
    }

    @Override
    public DelinquentAccount importAccount(DelinquentAccount account, String userId) {
        int dpd = account.getDpd() == null ? 0 : account.getDpd();
        account.setDpd(dpd);
        account.setBucket(classifyBucket(dpd));
        if (account.getStatus() == null) account.setStatus(AccountStatus.ACTIVE);
        if (account.getDaysInCurrentBucket() == null) account.setDaysInCurrentBucket(0);
        // Attempt auto-allocation to a collections agent before the first save.
        allocationService.autoAllocate(account);
        DelinquentAccount saved = repo.save(account);
        log.info("Imported account loanRef={} bucket={} agent={} by {}",
                saved.getLoanRef(), saved.getBucket(), saved.getAssignedAgentId(), userId);
        return saved;
    }

    @Override
    @Transactional
    public DelinquentAccount onboard(CreateAccountRequest req, String userId) {
        // Unique loan reference — checked up front so callers get a clean message, not a DB error.
        if (repo.existsByLoanRef(req.loanRef())) {
            throw new BusinessRuleException("Duplicate loan reference: " + req.loanRef(), "DUPLICATE_LOAN_REF");
        }
        boolean secured = Boolean.TRUE.equals(req.secured());
        boolean hasCollateral = req.assetType() != null && req.estimatedValue() != null;
        // A secured loan cannot exist without its collateral.
        if (secured && !hasCollateral) {
            throw new BusinessRuleException(
                    "A secured loan requires collateral (asset type and estimated value).",
                    "COLLATERAL_REQUIRED");
        }

        DelinquentAccount account = mapper.toEntity(req);
        account.setSecured(secured);
        DelinquentAccount saved = importAccount(account, userId);

        // Persist the collateral in the same transaction, so a secured loan is never left without it.
        if (hasCollateral) {
            CollateralAsset asset = CollateralAsset.builder()
                    .accountId(saved.getAccountId())
                    .assetType(req.assetType())
                    .description(req.assetDescription())
                    .estimatedValue(req.estimatedValue())
                    .verificationStatus(VerificationStatus.VERIFIED)
                    .lastVerifiedDate(req.lastVerifiedDate() != null
                            ? req.lastVerifiedDate().atStartOfDay() : java.time.LocalDateTime.now())
                    .build();
            collateralRepo.save(asset);
            log.info("Collateral {} ({}) registered with account {}",
                    asset.getAssetId(), asset.getAssetType(), saved.getAccountId());
        }
        return saved;
    }

    @Override
    public DpdBucket classifyBucket(int dpd) {
        return DpdBucket.classify(dpd);
    }

    @Override
    public DelinquentAccount update(String id, UpdateAccountRequest req) {
        DelinquentAccount account = find(id);
        if (req.borrowerName() != null) account.setBorrowerName(req.borrowerName());
        if (req.phone() != null) account.setPhone(req.phone());
        if (req.address() != null) account.setAddress(req.address());
        if (req.branchId() != null) account.setBranchId(req.branchId());
        if (req.principalAmount() != null) account.setPrincipalAmount(req.principalAmount());
        if (req.totalOverdue() != null) account.setTotalOverdue(req.totalOverdue());
        if (req.dpd() != null) {
            account.setDpd(req.dpd());
            account.setBucket(classifyBucket(req.dpd()));
        }
        if (req.daysInCurrentBucket() != null) account.setDaysInCurrentBucket(req.daysInCurrentBucket());
        if (req.status() != null) account.setStatus(req.status());
        // Manual (admin) re-assignment: a blank value clears the assignment, leaving it for allocation.
        if (req.assignedAgentId() != null) {
            account.setAssignedAgentId(req.assignedAgentId().isBlank() ? null : req.assignedAgentId().trim());
        }
        DelinquentAccount saved = repo.save(account);
        log.info("Updated account id={} bucket={} status={}", saved.getAccountId(), saved.getBucket(), saved.getStatus());
        return saved;
    }

    @Override
    public void delete(String id) {
        DelinquentAccount account = find(id);
        repo.delete(account);
        log.info("Deleted account id={}", id);
    }

    @Override
    public DelinquentAccount assignAgent(String id, String agentId) {
        DelinquentAccount account = find(id);
        if (!authClient.userExists(agentId)) {
            throw new BusinessRuleException("Agent does not exist: " + agentId, "UNKNOWN_AGENT");
        }
        account.setAssignedAgentId(agentId);
        DelinquentAccount saved = repo.save(account);
        log.info("Assigned account id={} to agent={}", id, agentId);
        return saved;
    }

    @Override
    public DelinquentAccount updateStatus(String id, AccountStatus status) {
        DelinquentAccount account = find(id);
        account.setStatus(status);
        DelinquentAccount saved = repo.save(account);
        log.info("Account id={} status -> {}", id, status);
        return saved;
    }

    @Override
    public boolean exists(String id) {
        return repo.existsById(id);
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> byBucket = new LinkedHashMap<>();
        for (DpdBucket b : DpdBucket.values()) {
            byBucket.put(b.name(), repo.countByBucket(b));
        }
        BigDecimal totalOverdue = repo.sumTotalOverdue();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalAccounts", repo.count());
        stats.put("activeAccounts", repo.countByStatus(AccountStatus.ACTIVE));
        stats.put("settledAccounts", repo.countByStatus(AccountStatus.SETTLED));
        stats.put("legalAccounts", repo.countByStatus(AccountStatus.LEGAL));
        stats.put("writeOffAccounts", repo.countByStatus(AccountStatus.WRITEOFF));
        stats.put("totalOverdue", totalOverdue == null ? BigDecimal.ZERO : totalOverdue);
        stats.put("byBucket", byBucket);
        return stats;
    }

    private DelinquentAccount find(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }
}

package com.debtpulse.account.controller;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.DpdBucket;
import com.debtpulse.account.dto.request.CreateAccountRequest;
import com.debtpulse.account.dto.request.UpdateAccountRequest;
import com.debtpulse.account.entity.DelinquentAccount;
import com.debtpulse.account.feign.AuthClient;
import com.debtpulse.account.feign.dto.AuditLogRequest;
import com.debtpulse.account.mapper.AccountMapper;
import com.debtpulse.account.service.AccountService;
import com.debtpulse.common.dto.PageResponse;
import com.debtpulse.account.exception.BusinessRuleException;
import com.debtpulse.common.security.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT')")
@Tag(name = "Accounts", description = "Delinquent account portfolio management")
public class AccountController {

    private static final String ENTITY_TYPE = "DelinquentAccount";
    private static final String SOURCE_SERVICE = "account-service";

    private final AccountService accountService;
    private final AccountMapper mapper;
    private final AuthClient authClient;

    public AccountController(AccountService accountService, AccountMapper mapper, AuthClient authClient) {
        this.accountService = accountService;
        this.mapper = mapper;
        this.authClient = authClient;
    }

    @GetMapping
    @Operation(summary = "List delinquent accounts (paginated, filterable, sorted by DPD desc)")
    public ResponseEntity<PageResponse<DelinquentAccount>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) DpdBucket bucket,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) Integer dpdMin,
            @RequestParam(required = false) Integer dpdMax) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dpd"));
        return ResponseEntity.ok(PageResponse.of(
                accountService.list(bucket, status, agentId, dpdMin, dpdMax, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DelinquentAccount> getById(@PathVariable String id) {
        return ResponseEntity.ok(accountService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a delinquent account (bucket derived, auto-allocated)")
    public ResponseEntity<DelinquentAccount> create(@Valid @RequestBody CreateAccountRequest req) {
        DelinquentAccount saved = accountService.importAccount(mapper.toEntity(req), AuthContext.currentUserId());
        audit("CREATE", saved.getAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk-import accounts from a CSV (loanRef,borrowerName,phone,address,principal,overdue,dpd,[branchId])")
    public ResponseEntity<Map<String, Object>> importCsv(@RequestPart("file") MultipartFile file) {
        List<String> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String userId = AuthContext.currentUserId();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1) continue;            // skip header
                if (line.isBlank()) continue;
                String[] c = line.split(",", -1);
                try {
                    if (c.length < 7) {
                        throw new IllegalArgumentException("expected at least 7 columns, got " + c.length);
                    }
                    DelinquentAccount account = DelinquentAccount.builder()
                            .loanRef(c[0].trim())
                            .borrowerName(c[1].trim())
                            .phone(c[2].trim())
                            .address(c[3].trim())
                            .principalAmount(new BigDecimal(c[4].trim()))
                            .totalOverdue(new BigDecimal(c[5].trim()))
                            .dpd(Integer.parseInt(c[6].trim()))
                            .branchId(c.length >= 8 && !c[7].isBlank() ? c[7].trim() : null)
                            .build();
                    DelinquentAccount saved = accountService.importAccount(account, userId);
                    imported.add(saved.getLoanRef());
                } catch (Exception ex) {
                    errors.add("line " + lineNo + ": " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            throw new BusinessRuleException("Failed to read CSV: " + ex.getMessage(), "CSV_READ_ERROR");
        }

        audit("CSV_IMPORT", "count=" + imported.size());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("importedCount", imported.size());
        summary.put("errorCount", errors.size());
        summary.put("imported", imported);
        summary.put("errors", errors);
        return ResponseEntity.ok(summary);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DelinquentAccount> update(@PathVariable String id,
                                                    @Valid @RequestBody UpdateAccountRequest req) {
        DelinquentAccount saved = accountService.update(id, req);
        audit("UPDATE", id);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        accountService.delete(id);
        audit("DELETE", id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/assign-agent/{agentId}")
    @Operation(summary = "Assign an account to a collections agent")
    public ResponseEntity<DelinquentAccount> assignAgent(@PathVariable String id,
                                                         @PathVariable String agentId) {
        DelinquentAccount saved = accountService.assignAgent(id, agentId);
        audit("ASSIGN_AGENT", id);
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DelinquentAccount> updateStatus(@PathVariable String id,
                                                          @RequestParam AccountStatus status) {
        DelinquentAccount saved = accountService.updateStatus(id, status);
        audit("STATUS_UPDATE", id);
        return ResponseEntity.ok(saved);
    }

    private void audit(String action, String recordId) {
        authClient.audit(new AuditLogRequest(
                AuthContext.currentUserId(), action, ENTITY_TYPE, recordId, SOURCE_SERVICE));
    }
}

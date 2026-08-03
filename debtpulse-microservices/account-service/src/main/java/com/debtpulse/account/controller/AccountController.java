package com.debtpulse.account.controller;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.AssetType;
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
        DelinquentAccount saved = accountService.onboard(req, AuthContext.currentUserId());
        audit("CREATE", saved.getAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk-import accounts from a CSV (loanRef,borrowerName,phone,address,"
            + "principal,overdue,dpd,[branchId,secured,assetType,assetDescription,estimatedValue])")
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
                try {
                    CreateAccountRequest req = parseCsvRow(line.split(",", -1));
                    imported.add(accountService.onboard(req, userId).getLoanRef());
                } catch (BusinessRuleException | IllegalArgumentException ex) {
                    // Surface a clean, human-readable reason — never a raw SQL/driver message.
                    errors.add("line " + lineNo + ": " + ex.getMessage());
                } catch (Exception ex) {
                    errors.add("line " + lineNo + ": could not import this row");
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

    /** Parse one CSV row into an onboarding request, throwing a friendly message on any bad cell. */
    private CreateAccountRequest parseCsvRow(String[] c) {
        if (c.length < 7) {
            throw new IllegalArgumentException("expected at least 7 columns, got " + c.length);
        }
        String loanRef = c[0].trim();
        if (loanRef.isBlank()) throw new IllegalArgumentException("loan reference is required");
        String borrowerName = c[1].trim();
        if (borrowerName.isBlank()) throw new IllegalArgumentException("borrower name is required");

        BigDecimal principal = parseAmount(c[4], "principal");
        BigDecimal overdue = parseAmount(c[5], "overdue");
        int dpd = parseInt(c[6], "dpd");
        String branchId = c.length >= 8 && !c[7].isBlank() ? c[7].trim() : null;
        boolean secured = c.length >= 9 && parseBool(c[8]);
        AssetType assetType = c.length >= 10 && !c[9].isBlank() ? parseAssetType(c[9]) : null;
        String assetDesc = c.length >= 11 && !c[10].isBlank() ? c[10].trim() : null;
        BigDecimal estValue = c.length >= 12 && !c[11].isBlank() ? parseAmount(c[11], "estimatedValue") : null;

        return new CreateAccountRequest(loanRef, borrowerName, c[2].trim(), c[3].trim(), branchId,
                principal, overdue, dpd, secured, assetType, assetDesc, estValue);
    }

    private static BigDecimal parseAmount(String raw, String field) {
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid number for " + field + ": '" + raw.trim() + "'");
        }
    }

    private static int parseInt(String raw, String field) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid whole number for " + field + ": '" + raw.trim() + "'");
        }
    }

    private static boolean parseBool(String raw) {
        String v = raw.trim().toLowerCase();
        if (v.isEmpty() || v.equals("false") || v.equals("no") || v.equals("0")) return false;
        if (v.equals("true") || v.equals("yes") || v.equals("1")) return true;
        throw new IllegalArgumentException("secured must be true or false, got '" + raw.trim() + "'");
    }

    private static AssetType parseAssetType(String raw) {
        try {
            return AssetType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown asset type '" + raw.trim()
                    + "' (expected one of PROPERTY, VEHICLE, GOLD, MACHINERY, STOCKS)");
        }
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
        com.debtpulse.common.audit.AuditContext.markRecorded();
    }
}

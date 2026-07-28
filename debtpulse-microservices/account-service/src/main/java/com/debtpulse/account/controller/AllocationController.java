package com.debtpulse.account.controller;

import com.debtpulse.account.dto.request.AllocationRuleRequest;
import com.debtpulse.account.entity.AllocationRule;
import com.debtpulse.account.feign.AuthClient;
import com.debtpulse.account.feign.dto.AuditLogRequest;
import com.debtpulse.account.service.AllocationService;
import com.debtpulse.common.dto.PageResponse;
import com.debtpulse.common.security.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/allocations")
@PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT','PORTFOLIO_MANAGER')")
@Tag(name = "Allocations", description = "Allocation rules and the allocation engine")
public class AllocationController {

    private static final String ENTITY_TYPE = "AllocationRule";
    private static final String SOURCE_SERVICE = "account-service";

    private final AllocationService allocationService;
    private final AuthClient authClient;

    public AllocationController(AllocationService allocationService, AuthClient authClient) {
        this.allocationService = allocationService;
        this.authClient = authClient;
    }

    @GetMapping
    @Operation(summary = "List allocation rules (paginated, by priority desc)")
    public ResponseEntity<PageResponse<AllocationRule>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "priority"));
        return ResponseEntity.ok(PageResponse.of(allocationService.listRules(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AllocationRule> getById(@PathVariable String id) {
        return ResponseEntity.ok(allocationService.getRule(id));
    }

    @PostMapping
    @Operation(summary = "Create an allocation/escalation rule")
    public ResponseEntity<AllocationRule> create(@Valid @RequestBody AllocationRuleRequest req) {
        AllocationRule saved = allocationService.createRule(req);
        audit("CREATE", saved.getRuleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AllocationRule> update(@PathVariable String id,
                                                 @Valid @RequestBody AllocationRuleRequest req) {
        AllocationRule saved = allocationService.updateRule(id, req);
        audit("UPDATE", id);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        allocationService.deleteRule(id);
        audit("DELETE", id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/execute")
    @Operation(summary = "Allocate all unassigned ACTIVE accounts to active collections agents")
    public ResponseEntity<Map<String, Object>> execute() {
        Map<String, Object> summary = allocationService.executeAllocation();
        audit("EXECUTE_ALLOCATION", "assigned=" + summary.get("assigned"));
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/escalate")
    @PreAuthorize("hasAnyRole('ADMIN','PORTFOLIO_MANAGER')")
    @Operation(summary = "Run the rule-driven escalation engine now (same logic as the nightly EscalationScheduler): "
            + "re-evaluate every ACTIVE account against the active rules and reassign stagnating ones to their "
            + "target role. Lets you verify scheduler behaviour on demand after setting daysInCurrentBucket.")
    public ResponseEntity<Map<String, Object>> escalate() {
        int reassigned = allocationService.reassignForEscalation();
        audit("ESCALATE_RUN", "reassigned=" + reassigned);
        return ResponseEntity.ok(Map.of("reassigned", reassigned));
    }

    private void audit(String action, String recordId) {
        authClient.audit(new AuditLogRequest(
                AuthContext.currentUserId(), action, ENTITY_TYPE, recordId, SOURCE_SERVICE));
        com.debtpulse.common.audit.AuditContext.markRecorded();
    }
}

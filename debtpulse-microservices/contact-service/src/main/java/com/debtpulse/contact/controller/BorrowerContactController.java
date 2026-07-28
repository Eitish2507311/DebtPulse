package com.debtpulse.contact.controller;

import com.debtpulse.common.dto.PageResponse;
import com.debtpulse.contact.dto.request.BorrowerContactRequest;
import com.debtpulse.contact.dto.response.BorrowerContactDto;
import com.debtpulse.contact.service.BorrowerContactService;
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

import java.util.List;

@RestController
@RequestMapping("/api/borrower-contacts")
@Tag(name = "Borrower Contacts", description = "Stored borrower contact records (2.3)")
public class BorrowerContactController {

    private final BorrowerContactService service;

    public BorrowerContactController(BorrowerContactService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT')")
    @Operation(summary = "Create a borrower contact record")
    public ResponseEntity<BorrowerContactDto> create(@Valid @RequestBody BorrowerContactRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT','PORTFOLIO_MANAGER')")
    @Operation(summary = "List borrower contacts (paginated)")
    public ResponseEntity<PageResponse<BorrowerContactDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId"));
        return ResponseEntity.ok(PageResponse.of(service.list(pageable)));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT','PORTFOLIO_MANAGER')")
    @Operation(summary = "List all contact records for an account")
    public ResponseEntity<List<BorrowerContactDto>> byAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(service.listByAccount(accountId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT','PORTFOLIO_MANAGER')")
    public ResponseEntity<BorrowerContactDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT')")
    public ResponseEntity<BorrowerContactDto> update(@PathVariable String id,
                                                     @Valid @RequestBody BorrowerContactRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

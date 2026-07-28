package com.debtpulse.contact.controller;

import com.debtpulse.common.dto.PageResponse;
import com.debtpulse.common.enums.ContactChannel;
import com.debtpulse.common.enums.ContactOutcome;
import com.debtpulse.contact.dto.request.ContactAttemptRequest;
import com.debtpulse.contact.dto.response.ContactAttemptDto;
import com.debtpulse.contact.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/contacts")
@Tag(name = "Contacts", description = "Borrower contact-attempt logging (2.3)")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PORTFOLIO_MANAGER','COLLECTIONS_AGENT')")
    @Operation(summary = "Log a contact attempt")
    public ResponseEntity<ContactAttemptDto> create(@Valid @RequestBody ContactAttemptRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.create(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT','PORTFOLIO_MANAGER')")
    public ResponseEntity<ContactAttemptDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(contactService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PORTFOLIO_MANAGER')")
    public ResponseEntity<ContactAttemptDto> update(@PathVariable String id,
                                                    @Valid @RequestBody ContactAttemptRequest req) {
        return ResponseEntity.ok(contactService.update(id, req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT','PORTFOLIO_MANAGER')")
    @Operation(summary = "List contact attempts (paginated, optional accountId/agentId/channel/outcome/date-range filters)")
    public ResponseEntity<PageResponse<ContactAttemptDto>> list(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) ContactChannel channel,
            @RequestParam(required = false) ContactOutcome outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "contactDate"));
        return ResponseEntity.ok(PageResponse.of(
                contactService.list(accountId, agentId, channel, outcome, from, to, pageable)));
    }
}

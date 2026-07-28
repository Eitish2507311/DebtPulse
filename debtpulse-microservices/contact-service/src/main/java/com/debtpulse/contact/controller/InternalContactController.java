package com.debtpulse.contact.controller;

import com.debtpulse.contact.service.ContactService;
import com.debtpulse.contact.service.PtpService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal API consumed by other microservices via Feign (INTERNAL_CONTRACTS.md).
 * Not routed through the public gateway; reached only service-to-service with propagated
 * identity.
 */
@RestController
@RequestMapping("/api/internal")
@Tag(name = "Internal - Contact", description = "Service-to-service PTP/contact stats (Feign)")
public class InternalContactController {

    private final PtpService ptpService;
    private final ContactService contactService;

    public InternalContactController(PtpService ptpService, ContactService contactService) {
        this.ptpService = ptpService;
        this.contactService = contactService;
    }

    @GetMapping("/ptp/active-count")
    public ResponseEntity<Long> activePtpCount(@RequestParam String accountId) {
        return ResponseEntity.ok(ptpService.activeCount(accountId));
    }

    @GetMapping("/ptp/stats")
    public ResponseEntity<Map<String, Object>> ptpStats() {
        return ResponseEntity.ok(ptpService.stats());
    }

    @GetMapping("/contacts/stats")
    public ResponseEntity<Map<String, Object>> contactStats() {
        return ResponseEntity.ok(contactService.stats());
    }
}

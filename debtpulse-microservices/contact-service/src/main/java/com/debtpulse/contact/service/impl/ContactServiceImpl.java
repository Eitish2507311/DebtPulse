package com.debtpulse.contact.service.impl;

import com.debtpulse.contact.exception.BusinessRuleException;
import com.debtpulse.contact.exception.ResourceNotFoundException;
import com.debtpulse.common.security.AuthContext;
import com.debtpulse.common.enums.ContactStatus;
import com.debtpulse.common.enums.Role;
import com.debtpulse.contact.dto.request.ContactAttemptRequest;
import com.debtpulse.contact.dto.response.ContactAttemptDto;
import com.debtpulse.common.enums.ContactChannel;
import com.debtpulse.common.enums.ContactOutcome;
import com.debtpulse.contact.entity.ContactAttempt;
import com.debtpulse.contact.feign.AccountClient;
import com.debtpulse.contact.feign.AuthClient;
import com.debtpulse.contact.feign.dto.AuditLogRequest;
import com.debtpulse.contact.mapper.ContactAttemptMapper;
import com.debtpulse.contact.repository.ContactAttemptRepository;
import com.debtpulse.contact.repository.ContactSpecifications;
import com.debtpulse.contact.service.ContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ContactServiceImpl implements ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactServiceImpl.class);
    private static final String SOURCE_SERVICE = "contact-service";

    private final ContactAttemptRepository repo;
    private final ContactAttemptMapper mapper;
    private final AccountClient accountClient;
    private final AuthClient authClient;

    public ContactServiceImpl(ContactAttemptRepository repo, ContactAttemptMapper mapper,
                              AccountClient accountClient, AuthClient authClient) {
        this.repo = repo;
        this.mapper = mapper;
        this.accountClient = accountClient;
        this.authClient = authClient;
    }

    @Override
    public ContactAttemptDto create(ContactAttemptRequest req) {
        if (!accountClient.accountExists(req.accountId())) {
            throw new BusinessRuleException("Account not found: " + req.accountId(), "ACCOUNT_NOT_FOUND");
        }
        // A collections agent always logs the attempt under their own id; managers/admins may
        // record on behalf of an agent supplied in the request.
        String agentId = resolveAgentId(req.agentId());
        ContactAttempt entity = ContactAttempt.builder()
                .accountId(req.accountId())
                .agentId(agentId)
                .contactDate(req.contactDate() != null ? req.contactDate() : LocalDateTime.now())
                .channel(req.channel())
                .outcome(req.outcome())
                .notes(req.notes())
                .status(ContactStatus.LOGGED)
                .build();
        ContactAttempt saved = repo.save(entity);
        log.info("Contact attempt logged id={} account={} channel={} outcome={}",
                saved.getContactId(), saved.getAccountId(), saved.getChannel(), saved.getOutcome());
        audit("CREATE", saved.getContactId());
        return mapper.toDto(saved);
    }

    @Override
    public ContactAttemptDto getById(String id) {
        return mapper.toDto(find(id));
    }

    @Override
    public ContactAttemptDto update(String id, ContactAttemptRequest req) {
        ContactAttempt entity = find(id);
        if (req.channel() != null) entity.setChannel(req.channel());
        if (req.outcome() != null) entity.setOutcome(req.outcome());
        if (req.contactDate() != null) entity.setContactDate(req.contactDate());
        if (req.notes() != null) entity.setNotes(req.notes());
        if (req.agentId() != null) entity.setAgentId(req.agentId());
        ContactAttempt saved = repo.save(entity);
        log.info("Contact attempt updated id={}", id);
        audit("UPDATE", id);
        return mapper.toDto(saved);
    }

    @Override
    public Page<ContactAttemptDto> list(String accountId, String agentId, ContactChannel channel,
                                        ContactOutcome outcome, LocalDate from, LocalDate to,
                                        Pageable pageable) {
        return repo.findAll(
                ContactSpecifications.withFilters(accountId, agentId, channel, outcome, from, to), pageable)
                .map(mapper::toDto);
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalContacts", repo.count());
        stats.put("connectedContacts", repo.countByOutcome(ContactOutcome.CONNECTED));
        return stats;
    }

    private String resolveAgentId(String requestedAgentId) {
        if (Role.COLLECTIONS_AGENT.name().equals(AuthContext.currentRole())) {
            return AuthContext.currentUserId();
        }
        return requestedAgentId;
    }

    private ContactAttempt find(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact attempt not found: " + id));
    }

    private void audit(String action, String recordId) {
        authClient.audit(new AuditLogRequest(
                AuthContext.currentUserId(), action, "ContactAttempt", recordId, SOURCE_SERVICE));
    }
}

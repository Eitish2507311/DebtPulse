package com.debtpulse.contact.service.impl;

import com.debtpulse.contact.exception.BusinessRuleException;
import com.debtpulse.contact.exception.ResourceNotFoundException;
import com.debtpulse.common.security.AuthContext;
import com.debtpulse.common.enums.BorrowerContactStatus;
import com.debtpulse.contact.dto.request.BorrowerContactRequest;
import com.debtpulse.contact.dto.response.BorrowerContactDto;
import com.debtpulse.contact.entity.BorrowerContact;
import com.debtpulse.contact.feign.AccountClient;
import com.debtpulse.contact.feign.AuthClient;
import com.debtpulse.contact.feign.dto.AuditLogRequest;
import com.debtpulse.contact.mapper.BorrowerContactMapper;
import com.debtpulse.contact.repository.BorrowerContactRepository;
import com.debtpulse.contact.service.BorrowerContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BorrowerContactServiceImpl implements BorrowerContactService {

    private static final Logger log = LoggerFactory.getLogger(BorrowerContactServiceImpl.class);
    private static final String SOURCE_SERVICE = "contact-service";

    private final BorrowerContactRepository repo;
    private final BorrowerContactMapper mapper;
    private final AuthClient authClient;
    private final AccountClient accountClient;

    public BorrowerContactServiceImpl(BorrowerContactRepository repo, BorrowerContactMapper mapper,
                                      AuthClient authClient, AccountClient accountClient) {
        this.repo = repo;
        this.mapper = mapper;
        this.authClient = authClient;
        this.accountClient = accountClient;
    }

    @Override
    public BorrowerContactDto create(BorrowerContactRequest req) {
        // A borrower contact must belong to a real, registered delinquent account.
        if (!accountClient.accountExists(req.accountId())) {
            throw new BusinessRuleException("Account not found: " + req.accountId(), "ACCOUNT_NOT_FOUND");
        }
        BorrowerContact entity = BorrowerContact.builder()
                .accountId(req.accountId())
                .contactType(req.contactType())
                .name(req.name())
                .phone(req.phone())
                .relationship(req.relationship())
                .status(req.status() != null ? req.status() : BorrowerContactStatus.ACTIVE)
                .build();
        BorrowerContact saved = repo.save(entity);
        log.info("Borrower contact created id={} account={} type={}",
                saved.getContactRecordId(), saved.getAccountId(), saved.getContactType());
        audit("CREATE", saved.getContactRecordId());
        return mapper.toDto(saved);
    }

    @Override
    public BorrowerContactDto getById(String id) {
        return mapper.toDto(find(id));
    }

    @Override
    public BorrowerContactDto update(String id, BorrowerContactRequest req) {
        BorrowerContact entity = find(id);
        if (req.contactType() != null) entity.setContactType(req.contactType());
        if (req.name() != null) entity.setName(req.name());
        if (req.phone() != null) entity.setPhone(req.phone());
        if (req.relationship() != null) entity.setRelationship(req.relationship());
        if (req.status() != null) entity.setStatus(req.status());
        BorrowerContact saved = repo.save(entity);
        log.info("Borrower contact updated id={}", id);
        audit("UPDATE", id);
        return mapper.toDto(saved);
    }

    @Override
    public void delete(String id) {
        BorrowerContact entity = find(id);
        repo.delete(entity);
        log.info("Borrower contact deleted id={}", id);
        audit("DELETE", id);
    }

    @Override
    public Page<BorrowerContactDto> list(Pageable pageable) {
        return repo.findAll(pageable).map(mapper::toDto);
    }

    @Override
    public List<BorrowerContactDto> listByAccount(String accountId) {
        return repo.findByAccountId(accountId).stream().map(mapper::toDto).toList();
    }

    private BorrowerContact find(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower contact not found: " + id));
    }

    private void audit(String action, String recordId) {
        authClient.audit(new AuditLogRequest(
                AuthContext.currentUserId(), action, "BorrowerContact", recordId, SOURCE_SERVICE));
        com.debtpulse.common.audit.AuditContext.markRecorded();
    }
}

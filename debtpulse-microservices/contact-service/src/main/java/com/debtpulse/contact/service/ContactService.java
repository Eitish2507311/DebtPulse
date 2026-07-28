package com.debtpulse.contact.service;

import com.debtpulse.common.enums.ContactChannel;
import com.debtpulse.common.enums.ContactOutcome;
import com.debtpulse.contact.dto.request.ContactAttemptRequest;
import com.debtpulse.contact.dto.response.ContactAttemptDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

/** Contact-attempt logging and lookups (2.3 Contact &amp; Follow-Up Management). */
public interface ContactService {

    ContactAttemptDto create(ContactAttemptRequest request);

    ContactAttemptDto getById(String id);

    ContactAttemptDto update(String id, ContactAttemptRequest request);

    /**
     * Paginated list; every filter is optional (null/blank = ignored) and applied conjunctively.
     * {@code from}/{@code to} bound the contact date (inclusive).
     */
    Page<ContactAttemptDto> list(String accountId, String agentId, ContactChannel channel,
                                 ContactOutcome outcome, LocalDate from, LocalDate to, Pageable pageable);

    /** Internal stats: {@code totalContacts, connectedContacts}. */
    Map<String, Object> stats();
}

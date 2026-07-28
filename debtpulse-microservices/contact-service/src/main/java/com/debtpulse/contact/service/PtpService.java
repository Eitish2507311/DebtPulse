package com.debtpulse.contact.service;

import com.debtpulse.common.enums.PtpStatus;
import com.debtpulse.contact.dto.request.PtpRequest;
import com.debtpulse.contact.dto.response.PtpDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Promise-to-pay management (2.3 Contact &amp; Follow-Up Management). */
public interface PtpService {

    PtpDto create(PtpRequest request);

    PtpDto getById(String id);

    /**
     * Paginated list; every filter is optional (null/blank = ignored) and applied conjunctively.
     * {@code from}/{@code to} bound the commitment date (inclusive).
     */
    Page<PtpDto> list(String accountId, String agentId, PtpStatus status,
                      LocalDate from, LocalDate to, Pageable pageable);

    /** Records a payment; KEPT when it covers the promised amount, otherwise PARTIAL. */
    PtpDto recordPayment(String id, BigDecimal actualPaidAmount);

    /** Moves the commitment to a new date and marks the PTP RESCHEDULED. */
    PtpDto reschedule(String id, LocalDate newCommitmentDate);

    /** Count of ACTIVE PTPs for an account (internal Feign endpoint). */
    long activeCount(String accountId);

    /** Internal stats: {@code totalPtp, activePtp, keptPtp, brokenPtp, ptpBreachRate}. */
    Map<String, Object> stats();

    /** Daily sweep: mark lapsed ACTIVE PTPs BROKEN and notify their agents. Returns count. */
    int markBreachedPtps();
}

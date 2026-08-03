package com.debtpulse.legal.service;

import com.debtpulse.legal.dto.request.CourtHearingRequest;
import com.debtpulse.legal.dto.request.LegalCaseRequest;
import com.debtpulse.legal.dto.request.RecoveryOrderRequest;
import com.debtpulse.legal.dto.response.CourtHearingDto;
import com.debtpulse.legal.dto.response.LegalCaseDto;
import com.debtpulse.legal.dto.response.RecoveryOrderDto;
import com.debtpulse.common.enums.CaseStatus;
import com.debtpulse.common.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/** Legal Proceedings Management (2.6): legal cases, court hearings and recovery orders. */
public interface LegalService {

    // ---- cases ----
    LegalCaseDto initiateCase(LegalCaseRequest request);

    /** Legal cases, optionally filtered by lifecycle status and/or account id ({@code null} = no filter). */
    Page<LegalCaseDto> listCases(CaseStatus status, String accountId, Pageable pageable);

    LegalCaseDto getCase(String id);

    LegalCaseDto updateCase(String id, LegalCaseRequest request);

    // ---- hearings ----
    CourtHearingDto addHearing(CourtHearingRequest request);

    List<CourtHearingDto> listHearings(String caseId);

    /** All hearings across every case (for the standalone Hearings view), newest first. */
    List<CourtHearingDto> listAllHearings();

    // ---- orders ----
    RecoveryOrderDto issueOrder(RecoveryOrderRequest request);

    List<RecoveryOrderDto> listOrders();

    RecoveryOrderDto getOrder(String id);

    /** Advance a recovery order through its lifecycle (ISSUED → IN_EXECUTION → EXECUTED / CHALLENGED / VACATED). */
    RecoveryOrderDto updateOrderStatus(String id, OrderStatus status);

    void deleteOrder(String id);

    // ---- internal stats ----
    Map<String, Object> stats();
}

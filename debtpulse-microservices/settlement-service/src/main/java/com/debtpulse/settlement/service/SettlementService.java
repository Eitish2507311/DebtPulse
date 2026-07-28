package com.debtpulse.settlement.service;

import com.debtpulse.common.enums.ApprovalLevel;
import com.debtpulse.settlement.dto.request.ApprovalDecisionRequest;
import com.debtpulse.settlement.dto.request.SettlementRequest;
import com.debtpulse.settlement.dto.response.SettlementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/** Settlement proposal workflow: creation with haircut calc, submit, maker-checker decide, queries. */
public interface SettlementService {

    SettlementResponse create(SettlementRequest request);

    SettlementResponse submit(String id);

    Page<SettlementResponse> list(Pageable pageable);

    SettlementResponse getById(String id);

    List<SettlementResponse> outstanding();

    List<SettlementResponse> pastDeadline();

    List<SettlementResponse> approvalQueue();

    SettlementResponse update(String id, SettlementRequest request);

    SettlementResponse decide(String id, ApprovalLevel level, ApprovalDecisionRequest request);

    SettlementResponse markPaid(String id);

    Map<String, Object> stats();
}

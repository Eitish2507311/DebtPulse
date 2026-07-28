package com.debtpulse.settlement.service;

import com.debtpulse.settlement.dto.request.RestructuringRequest;
import com.debtpulse.settlement.dto.response.RestructuringResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** Restructuring proposal CRUD plus approve/reject transitions. */
public interface RestructuringService {

    RestructuringResponse create(RestructuringRequest request);

    Page<RestructuringResponse> list(Pageable pageable);

    RestructuringResponse getById(String id);

    List<RestructuringResponse> byAccount(String accountId);

    RestructuringResponse update(String id, RestructuringRequest request);

    RestructuringResponse approve(String id);

    RestructuringResponse reject(String id);
}

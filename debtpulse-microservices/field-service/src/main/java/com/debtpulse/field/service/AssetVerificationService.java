package com.debtpulse.field.service;

import com.debtpulse.field.dto.request.AssetVerificationRequest;
import com.debtpulse.field.dto.response.AssetVerificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** Asset-verification reporting; creating a report flags the collateral VERIFIED in account-service. */
public interface AssetVerificationService {

    AssetVerificationDto create(AssetVerificationRequest request);

    Page<AssetVerificationDto> list(Pageable pageable);

    List<AssetVerificationDto> byVisit(String visitId);

    AssetVerificationDto getById(String id);

    AssetVerificationDto update(String id, AssetVerificationRequest request);
}

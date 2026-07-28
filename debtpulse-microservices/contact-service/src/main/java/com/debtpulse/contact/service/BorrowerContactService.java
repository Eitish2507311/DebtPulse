package com.debtpulse.contact.service;

import com.debtpulse.contact.dto.request.BorrowerContactRequest;
import com.debtpulse.contact.dto.response.BorrowerContactDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** Borrower contact-record management (2.3 Contact &amp; Follow-Up Management). */
public interface BorrowerContactService {

    BorrowerContactDto create(BorrowerContactRequest request);

    BorrowerContactDto getById(String id);

    BorrowerContactDto update(String id, BorrowerContactRequest request);

    void delete(String id);

    Page<BorrowerContactDto> list(Pageable pageable);

    List<BorrowerContactDto> listByAccount(String accountId);
}

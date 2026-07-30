package com.debtpulse.legal.service;

import com.debtpulse.legal.exception.BusinessRuleException;
import com.debtpulse.common.enums.CaseStatus;
import com.debtpulse.common.enums.CaseType;
import com.debtpulse.common.enums.HearingOutcome;
import com.debtpulse.common.enums.OrderType;
import com.debtpulse.legal.dto.request.CourtHearingRequest;
import com.debtpulse.legal.dto.request.LegalCaseRequest;
import com.debtpulse.legal.dto.request.RecoveryOrderRequest;
import com.debtpulse.legal.dto.response.CourtHearingDto;
import com.debtpulse.legal.dto.response.LegalCaseDto;
import com.debtpulse.legal.entity.CourtHearing;
import com.debtpulse.legal.entity.LegalCase;
import com.debtpulse.legal.entity.RecoveryOrder;
import com.debtpulse.legal.feign.AccountClient;
import com.debtpulse.legal.feign.AuthClient;
import com.debtpulse.legal.feign.NotificationClient;
import com.debtpulse.legal.mapper.LegalMapper;
import com.debtpulse.legal.repository.CourtHearingRepository;
import com.debtpulse.legal.repository.LegalCaseRepository;
import com.debtpulse.legal.repository.RecoveryOrderRepository;
import com.debtpulse.legal.service.impl.LegalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegalServiceImplTest {

    @Mock private LegalCaseRepository caseRepo;
    @Mock private CourtHearingRepository hearingRepo;
    @Mock private RecoveryOrderRepository orderRepo;
    @Mock private AccountClient accountClient;
    @Mock private AuthClient authClient;
    @Mock private NotificationClient notificationClient;

    @Captor private ArgumentCaptor<LegalCase> caseCaptor;

    private LegalServiceImpl service;

    @BeforeEach
    void setUp() {
        // Use the real mapper so returned DTOs are populated for assertions.
        service = new LegalServiceImpl(caseRepo, hearingRepo, orderRepo, new LegalMapper(),
                accountClient, authClient, notificationClient);
    }

    private LegalCaseRequest caseRequest() {
        return new LegalCaseRequest("ACC-001", CaseType.CIVIL_SUIT,
                LocalDate.of(2026, 7, 1), "City Civil Court", "CS/2026/123", null);
    }

    @Test
    void initiateCase_validAccount_savesFiledCase() {
        when(accountClient.accountExists("ACC-001")).thenReturn(true);
        when(caseRepo.save(any(LegalCase.class))).thenAnswer(inv -> inv.getArgument(0));

        LegalCaseDto dto = service.initiateCase(caseRequest());

        verify(accountClient).accountExists("ACC-001");
        verify(caseRepo).save(caseCaptor.capture());
        assertThat(caseCaptor.getValue().getStatus()).isEqualTo(CaseStatus.FILED);
        assertThat(caseCaptor.getValue().getAccountId()).isEqualTo("ACC-001");
        assertThat(dto.status()).isEqualTo(CaseStatus.FILED);
        // create actions are audited centrally
        verify(authClient).audit(any());
    }

    @Test
    void initiateCase_unknownAccount_throwsAndDoesNotSave() {
        when(accountClient.accountExists("ACC-001")).thenReturn(false);

        assertThatThrownBy(() -> service.initiateCase(caseRequest()))
                .isInstanceOf(BusinessRuleException.class);

        verify(caseRepo, never()).save(any());
    }

    private LegalCase caseWith(CaseStatus status) {
        return LegalCase.builder()
                .caseId("CASE-1").accountId("ACC-001").caseNumber("CS/2026/123")
                .status(status).build();
    }

    @Test
    void addHearing_scheduleHearing_movesFiledCaseToHearingScheduled() {
        LegalCase existing = caseWith(CaseStatus.FILED);
        when(caseRepo.findById("CASE-1")).thenReturn(Optional.of(existing));
        when(hearingRepo.save(any(CourtHearing.class))).thenAnswer(inv -> inv.getArgument(0));

        // No outcome yet — the hearing is only being scheduled.
        CourtHearingRequest req = new CourtHearingRequest("CASE-1",
                LocalDate.of(2026, 8, 1), null, LocalDate.of(2026, 8, 1), "first hearing", null, null);

        CourtHearingDto dto = service.addHearing(req);

        assertThat(existing.getStatus()).isEqualTo(CaseStatus.HEARING_SCHEDULED);
        verify(caseRepo).save(existing);
        assertThat(dto.caseId()).isEqualTo("CASE-1");
    }

    @Test
    void addHearing_onConcludedCase_throwsAndSavesNothing() {
        LegalCase existing = caseWith(CaseStatus.SETTLED);
        when(caseRepo.findById("CASE-1")).thenReturn(Optional.of(existing));

        CourtHearingRequest req = new CourtHearingRequest("CASE-1",
                LocalDate.of(2026, 8, 1), null, LocalDate.of(2026, 8, 1), "late hearing", null, null);

        assertThatThrownBy(() -> service.addHearing(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("concluded");

        verify(hearingRepo, never()).save(any());
        verify(caseRepo, never()).save(any());
    }

    @Test
    void addHearing_orderPassed_onScheduledCase_decreesCaseAndIssuesOrder() {
        LegalCase existing = caseWith(CaseStatus.HEARING_SCHEDULED);
        when(caseRepo.findById("CASE-1")).thenReturn(Optional.of(existing));
        when(hearingRepo.save(any(CourtHearing.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepo.save(any(RecoveryOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        CourtHearingRequest req = new CourtHearingRequest("CASE-1",
                LocalDate.of(2026, 8, 1), HearingOutcome.ORDER_PASSED, null, "order passed",
                OrderType.ATTACHMENT_ORDER, LocalDate.of(2026, 9, 1));

        service.addHearing(req);

        assertThat(existing.getStatus()).isEqualTo(CaseStatus.DECREED);
        verify(caseRepo).save(existing);
        // The order is auto-issued so the case surfaces under Recovery Orders.
        verify(orderRepo).save(any(RecoveryOrder.class));
    }

    @Test
    void addHearing_orderPassed_withoutOrderDetails_throws() {
        LegalCase existing = caseWith(CaseStatus.HEARING_SCHEDULED);
        when(caseRepo.findById("CASE-1")).thenReturn(Optional.of(existing));

        CourtHearingRequest req = new CourtHearingRequest("CASE-1",
                LocalDate.of(2026, 8, 1), HearingOutcome.ORDER_PASSED, null, "order passed", null, null);

        assertThatThrownBy(() -> service.addHearing(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ORDER_PASSED");

        verify(orderRepo, never()).save(any());
    }

    @Test
    void addHearing_orderPassed_onFiledCase_rejectsIllegalJumpToDecreed() {
        LegalCase existing = caseWith(CaseStatus.FILED);
        when(caseRepo.findById("CASE-1")).thenReturn(Optional.of(existing));
        when(hearingRepo.save(any(CourtHearing.class))).thenAnswer(inv -> inv.getArgument(0));

        // A decree with no scheduled hearing is an illegal jump (FILED -> DECREED).
        CourtHearingRequest req = new CourtHearingRequest("CASE-1",
                LocalDate.of(2026, 8, 1), HearingOutcome.ORDER_PASSED, null, "order passed",
                OrderType.ATTACHMENT_ORDER, LocalDate.of(2026, 9, 1));

        assertThatThrownBy(() -> service.addHearing(req))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(existing.getStatus()).isEqualTo(CaseStatus.FILED);
        verify(caseRepo, never()).save(any());
        verify(orderRepo, never()).save(any());
    }

    @Test
    void addHearing_settledOutcome_settlesCase() {
        LegalCase existing = caseWith(CaseStatus.HEARING_SCHEDULED);
        when(caseRepo.findById("CASE-1")).thenReturn(Optional.of(existing));
        when(hearingRepo.save(any(CourtHearing.class))).thenAnswer(inv -> inv.getArgument(0));

        CourtHearingRequest req = new CourtHearingRequest("CASE-1",
                LocalDate.of(2026, 8, 1), HearingOutcome.SETTLED, null, "settled in court", null, null);

        service.addHearing(req);

        assertThat(existing.getStatus()).isEqualTo(CaseStatus.SETTLED);
        verify(caseRepo).save(existing);
    }

    @Test
    void issueOrder_onNonDecreedCase_throwsAndSavesNothing() {
        LegalCase existing = caseWith(CaseStatus.HEARING_SCHEDULED);
        when(caseRepo.findById("CASE-1")).thenReturn(Optional.of(existing));

        RecoveryOrderRequest req = new RecoveryOrderRequest("CASE-1",
                OrderType.ATTACHMENT_ORDER, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), null);

        assertThatThrownBy(() -> service.issueOrder(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DECREED");

        verify(orderRepo, never()).save(any());
    }

    @Test
    void issueOrder_onDecreedCase_saves() {
        LegalCase existing = caseWith(CaseStatus.DECREED);
        when(caseRepo.findById("CASE-1")).thenReturn(Optional.of(existing));
        when(orderRepo.save(any(RecoveryOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        RecoveryOrderRequest req = new RecoveryOrderRequest("CASE-1",
                OrderType.ATTACHMENT_ORDER, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), null);

        service.issueOrder(req);

        verify(orderRepo).save(any(RecoveryOrder.class));
    }
}

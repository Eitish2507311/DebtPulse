package com.debtpulse.contact.service;

import com.debtpulse.contact.exception.BusinessRuleException;
import com.debtpulse.common.enums.PtpStatus;
import com.debtpulse.contact.dto.request.PtpRequest;
import com.debtpulse.contact.entity.PromiseToPay;
import com.debtpulse.contact.feign.AccountClient;
import com.debtpulse.contact.feign.AuthClient;
import com.debtpulse.contact.feign.NotificationClient;
import com.debtpulse.contact.feign.dto.AccountDto;
import com.debtpulse.contact.feign.dto.NotificationRequest;
import com.debtpulse.contact.mapper.PtpMapper;
import com.debtpulse.contact.repository.PromiseToPayRepository;
import com.debtpulse.contact.service.impl.PtpServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PtpServiceImplTest {

    @Mock private PromiseToPayRepository repo;
    @Mock private PtpMapper mapper;
    @Mock private AccountClient accountClient;
    @Mock private NotificationClient notificationClient;
    @Mock private AuthClient authClient;

    @InjectMocks private PtpServiceImpl service;

    private PtpRequest request() {
        return new PtpRequest("ACC-1", "USR-002", LocalDate.now(),
                new BigDecimal("1000.00"), LocalDate.now().plusDays(7), null);
    }

    @Test
    void create_validatesAccount_savesAndNotifies() {
        when(accountClient.accountExists("ACC-1")).thenReturn(true);
        when(repo.save(any(PromiseToPay.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(request());

        verify(repo).save(any(PromiseToPay.class));
        verify(notificationClient).notify(any(NotificationRequest.class));
        verify(authClient).audit(any());
    }

    @Test
    void create_unknownAccount_throwsAndDoesNotSave() {
        when(accountClient.accountExists("ACC-1")).thenReturn(false);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessRuleException.class);

        verify(repo, never()).save(any());
        verify(notificationClient, never()).notify(any());
    }

    @Test
    void create_amountExceedsDue_throwsAndDoesNotSave() {
        PtpRequest req = new PtpRequest("ACC-1", "USR-002", LocalDate.now(),
                new BigDecimal("5000.00"), LocalDate.now().plusDays(7), null);
        when(accountClient.accountExists("ACC-1")).thenReturn(true);
        when(accountClient.getAccount("ACC-1")).thenReturn(new AccountDto(
                "ACC-1", null, null, null, null, null, null, new BigDecimal("1000.00"), null, null, null, null));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds the outstanding due");

        verify(repo, never()).save(any());
    }

    @Test
    void create_duplicateActivePtp_sameAmountAndDate_throws() {
        PtpRequest req = request();
        when(accountClient.accountExists("ACC-1")).thenReturn(true);
        PromiseToPay existing = PromiseToPay.builder()
                .ptpId("PTP-9").accountId("ACC-1").status(PtpStatus.ACTIVE)
                .ptpAmount(req.ptpAmount()).commitmentDate(req.commitmentDate()).build();
        when(repo.findByAccountIdAndStatus("ACC-1", PtpStatus.ACTIVE)).thenReturn(java.util.List.of(existing));

        assertThatThrownBy(() -> service.create(req)).isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void create_differentCommitmentDate_allowedEvenWithActivePtp() {
        PtpRequest req = request();
        when(accountClient.accountExists("ACC-1")).thenReturn(true);
        PromiseToPay existing = PromiseToPay.builder()
                .ptpId("PTP-9").accountId("ACC-1").status(PtpStatus.ACTIVE)
                .ptpAmount(req.ptpAmount()).commitmentDate(req.commitmentDate().plusDays(3)).build();
        when(repo.findByAccountIdAndStatus("ACC-1", PtpStatus.ACTIVE)).thenReturn(java.util.List.of(existing));
        when(repo.save(any(PromiseToPay.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(req);
        verify(repo).save(any(PromiseToPay.class));
    }

    @Test
    void update_editsAmountAndDate() {
        PromiseToPay ptp = PromiseToPay.builder().ptpId("PTP-3").accountId("ACC-1")
                .ptpAmount(new BigDecimal("500.00")).status(PtpStatus.ACTIVE).build();
        when(repo.findById("PTP-3")).thenReturn(Optional.of(ptp));
        when(repo.save(any(PromiseToPay.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update("PTP-3", request());

        assertThat(ptp.getPtpAmount()).isEqualByComparingTo("1000.00");
        verify(authClient).audit(any());
    }

    @Test
    void recordPayment_fullAmount_setsKept() {
        PromiseToPay ptp = PromiseToPay.builder()
                .ptpId("PTP-1").accountId("ACC-1").agentId("USR-002")
                .ptpAmount(new BigDecimal("1000.00")).status(PtpStatus.ACTIVE).build();
        when(repo.findById("PTP-1")).thenReturn(Optional.of(ptp));
        when(repo.save(any(PromiseToPay.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recordPayment("PTP-1", new BigDecimal("1000.00"));

        assertThat(ptp.getStatus()).isEqualTo(PtpStatus.KEPT);
        assertThat(ptp.getActualPaidAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void recordPayment_partialAmount_setsPartial() {
        PromiseToPay ptp = PromiseToPay.builder()
                .ptpId("PTP-2").accountId("ACC-1").agentId("USR-002")
                .ptpAmount(new BigDecimal("1000.00")).status(PtpStatus.ACTIVE).build();
        when(repo.findById("PTP-2")).thenReturn(Optional.of(ptp));
        when(repo.save(any(PromiseToPay.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recordPayment("PTP-2", new BigDecimal("400.00"));

        assertThat(ptp.getStatus()).isEqualTo(PtpStatus.PARTIAL);
        assertThat(ptp.getActualPaidAmount()).isEqualByComparingTo("400.00");
    }
}

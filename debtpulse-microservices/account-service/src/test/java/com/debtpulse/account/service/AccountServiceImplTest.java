package com.debtpulse.account.service;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.DpdBucket;
import com.debtpulse.account.entity.DelinquentAccount;
import com.debtpulse.account.feign.AuthClient;
import com.debtpulse.account.repository.DelinquentAccountRepository;
import com.debtpulse.account.service.impl.AccountServiceImpl;
import com.debtpulse.account.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock private DelinquentAccountRepository repo;
    @Mock private AllocationService allocationService;
    @Mock private AuthClient authClient;

    @InjectMocks private AccountServiceImpl accountService;

    @Test
    void importAccount_classifiesBucketAndSaves() {
        DelinquentAccount input = DelinquentAccount.builder()
                .loanRef("LN-100").borrowerName("Asha")
                .principalAmount(new BigDecimal("50000"))
                .totalOverdue(new BigDecimal("8000"))
                .dpd(45)
                .build();
        when(allocationService.autoAllocate(any(DelinquentAccount.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(repo.save(any(DelinquentAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        DelinquentAccount saved = accountService.importAccount(input, "USR-001");

        assertThat(saved.getBucket()).isEqualTo(DpdBucket.X60);
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(allocationService).autoAllocate(input);
        verify(repo).save(input);
    }

    @Test
    void classifyBucket_mapsBoundariesCorrectly() {
        assertThat(accountService.classifyBucket(30)).isEqualTo(DpdBucket.X30);
        assertThat(accountService.classifyBucket(200)).isEqualTo(DpdBucket.NPA);
    }

    @Test
    void getById_notFound_throws() {
        when(repo.findById("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getById("MISSING"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MISSING");
    }
}

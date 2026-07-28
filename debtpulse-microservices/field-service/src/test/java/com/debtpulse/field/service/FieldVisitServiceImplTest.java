package com.debtpulse.field.service;

import com.debtpulse.field.exception.BusinessRuleException;
import com.debtpulse.field.exception.UnauthorizedActionException;
import com.debtpulse.common.enums.VisitStatus;
import com.debtpulse.field.dto.request.CompleteVisitRequest;
import com.debtpulse.field.dto.request.ScheduleVisitRequest;
import com.debtpulse.field.dto.response.FieldVisitDto;
import com.debtpulse.field.entity.FieldVisit;
import com.debtpulse.field.feign.AccountClient;
import com.debtpulse.field.feign.AuthClient;
import com.debtpulse.field.feign.NotificationClient;
import com.debtpulse.field.feign.dto.NotificationRequest;
import com.debtpulse.field.feign.dto.UserDto;
import com.debtpulse.field.mapper.FieldVisitMapper;
import com.debtpulse.field.repository.FieldVisitRepository;
import com.debtpulse.field.service.impl.FieldVisitServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FieldVisitServiceImplTest {

    @Mock private FieldVisitRepository repo;
    @Mock private AccountClient accountClient;
    @Mock private AuthClient authClient;
    @Mock private NotificationClient notificationClient;

    private FieldVisitServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FieldVisitServiceImpl(repo, new FieldVisitMapper(),
                accountClient, authClient, notificationClient);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private UserDto officer(String id, String role, String status) {
        return new UserDto(id, "Ravi Kumar", "ravi@dp.com", "9876543212", role, "B01", status, null);
    }

    @Test
    void schedule_validatesSavesAndNotifies() {
        ScheduleVisitRequest req = new ScheduleVisitRequest("ACC-1", "USR-9",
                LocalDate.now().plusDays(2), "Collect documents");
        when(accountClient.accountExists("ACC-1")).thenReturn(true);
        when(authClient.getUser("USR-9")).thenReturn(officer("USR-9", "FIELD_OFFICER", "ACTIVE"));
        when(repo.save(any(FieldVisit.class))).thenAnswer(inv -> {
            FieldVisit v = inv.getArgument(0);
            v.setVisitId("VIS-1");
            return v;
        });

        FieldVisitDto dto = service.schedule(req);

        assertThat(dto.visitId()).isEqualTo("VIS-1");
        assertThat(dto.status()).isEqualTo("SCHEDULED");
        assertThat(dto.officerId()).isEqualTo("USR-9");
        verify(repo).save(any(FieldVisit.class));
        verify(notificationClient).notify(any(NotificationRequest.class));
        verify(authClient).audit(any());
    }

    @Test
    void schedule_unknownAccount_throwsAndDoesNotSave() {
        ScheduleVisitRequest req = new ScheduleVisitRequest("ACC-X", "USR-9",
                LocalDate.now().plusDays(1), null);
        when(accountClient.accountExists("ACC-X")).thenReturn(false);

        assertThatThrownBy(() -> service.schedule(req)).isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
        verify(notificationClient, never()).notify(any());
    }

    @Test
    void schedule_inactiveOfficer_throws() {
        ScheduleVisitRequest req = new ScheduleVisitRequest("ACC-1", "USR-9", LocalDate.now().plusDays(1), null);
        when(accountClient.accountExists("ACC-1")).thenReturn(true);
        when(authClient.getUser("USR-9")).thenReturn(officer("USR-9", "FIELD_OFFICER", "SUSPENDED"));

        assertThatThrownBy(() -> service.schedule(req)).isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void schedule_notAFieldOfficer_throws() {
        ScheduleVisitRequest req = new ScheduleVisitRequest("ACC-1", "USR-9", LocalDate.now().plusDays(1), null);
        when(accountClient.accountExists("ACC-1")).thenReturn(true);
        when(authClient.getUser("USR-9")).thenReturn(officer("USR-9", "COLLECTIONS_AGENT", "ACTIVE"));

        assertThatThrownBy(() -> service.schedule(req)).isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void complete_byAssignedOfficer_succeeds() {
        authenticateAs("USR-9", "FIELD_OFFICER");
        FieldVisit existing = FieldVisit.builder()
                .visitId("VIS-1").accountId("ACC-1").officerId("USR-9")
                .scheduledDate(LocalDate.now()).status(VisitStatus.SCHEDULED).build();
        when(repo.findById("VIS-1")).thenReturn(Optional.of(existing));
        when(repo.save(any(FieldVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        FieldVisitDto dto = service.complete("VIS-1",
                new CompleteVisitRequest(LocalDate.now(), true, true, "Borrower agreed", "Follow up"));

        assertThat(dto.status()).isEqualTo("COMPLETED");
        assertThat(dto.borrowerMet()).isTrue();
        verify(notificationClient).notify(any(NotificationRequest.class));
    }

    @Test
    void complete_byOtherOfficer_throwsForbidden() {
        authenticateAs("USR-OTHER", "FIELD_OFFICER"); // not the assigned officer
        FieldVisit existing = FieldVisit.builder()
                .visitId("VIS-1").accountId("ACC-1").officerId("USR-9")
                .scheduledDate(LocalDate.now()).status(VisitStatus.SCHEDULED).build();
        when(repo.findById("VIS-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.complete("VIS-1",
                new CompleteVisitRequest(LocalDate.now(), true, false, "x", null)))
                .isInstanceOf(UnauthorizedActionException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void complete_byAdmin_succeedsEvenIfNotOwner() {
        authenticateAs("USR-ADMIN", "ADMIN");
        FieldVisit existing = FieldVisit.builder()
                .visitId("VIS-1").accountId("ACC-1").officerId("USR-9")
                .scheduledDate(LocalDate.now()).status(VisitStatus.SCHEDULED).build();
        when(repo.findById("VIS-1")).thenReturn(Optional.of(existing));
        when(repo.save(any(FieldVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        FieldVisitDto dto = service.complete("VIS-1",
                new CompleteVisitRequest(LocalDate.now(), true, true, "supervisor override", null));

        assertThat(dto.status()).isEqualTo("COMPLETED");
    }

    @Test
    void markMissed_byOtherOfficer_throwsForbidden() {
        authenticateAs("USR-OTHER", "FIELD_OFFICER");
        FieldVisit existing = FieldVisit.builder()
                .visitId("VIS-2").accountId("ACC-1").officerId("USR-9")
                .scheduledDate(LocalDate.now()).status(VisitStatus.SCHEDULED).build();
        when(repo.findById("VIS-2")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.markMissed("VIS-2"))
                .isInstanceOf(UnauthorizedActionException.class);
        verify(repo, never()).save(any());
    }
}

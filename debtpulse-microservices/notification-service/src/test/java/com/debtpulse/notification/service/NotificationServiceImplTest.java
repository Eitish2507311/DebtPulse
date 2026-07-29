package com.debtpulse.notification.service;

import com.debtpulse.notification.exception.ResourceNotFoundException;
import com.debtpulse.notification.exception.UnauthorizedActionException;
import com.debtpulse.common.enums.NotifCategory;
import com.debtpulse.common.enums.NotifStatus;
import com.debtpulse.notification.dto.response.NotificationDto;
import com.debtpulse.notification.entity.Notification;
import com.debtpulse.notification.mapper.NotificationMapper;
import com.debtpulse.notification.repository.NotificationRepository;
import com.debtpulse.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository repo;

    // Real mapper — it is trivial, so exercising it gives better coverage than a mock.
    private final NotificationMapper mapper = new NotificationMapper();
    private NotificationServiceImpl service;

    private NotificationServiceImpl service() {
        if (service == null) service = new NotificationServiceImpl(repo, mapper);
        return service;
    }

    @Test
    void create_savesUnreadNotificationWithTimestamp() {
        when(repo.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setNotificationId("NOT-1");
            return n;
        });

        NotificationDto dto = service().create("USR-002", "Your PTP is due", NotifCategory.PTP);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repo).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(NotifStatus.UNREAD);
        assertThat(saved.getUserId()).isEqualTo("USR-002");
        assertThat(saved.getCategory()).isEqualTo(NotifCategory.PTP);
        assertThat(saved.getCreatedDate()).isNotNull();

        assertThat(dto.status()).isEqualTo("UNREAD");
        assertThat(dto.category()).isEqualTo("PTP");
        assertThat(dto.notificationId()).isEqualTo("NOT-1");
    }

    @Test
    void markRead_setsStatusRead() {
        Notification n = Notification.builder()
                .notificationId("NOT-1").userId("USR-002")
                .message("m").category(NotifCategory.LEGAL).status(NotifStatus.UNREAD).build();
        when(repo.findById("NOT-1")).thenReturn(Optional.of(n));
        when(repo.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationDto dto = service().markRead("USR-002", "NOT-1");

        assertThat(n.getStatus()).isEqualTo(NotifStatus.READ);
        assertThat(dto.status()).isEqualTo("READ");
    }

    @Test
    void markRead_notFound_throws() {
        when(repo.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().markRead("USR-002", "missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markRead_otherUsersNotification_throwsUnauthorized() {
        Notification n = Notification.builder()
                .notificationId("NOT-9").userId("USR-999")
                .message("m").category(NotifCategory.PTP).status(NotifStatus.UNREAD).build();
        when(repo.findById("NOT-9")).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service().markRead("USR-002", "NOT-9"))
                .isInstanceOf(UnauthorizedActionException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void dismiss_deletesTheNotification() {
        Notification n = Notification.builder()
                .notificationId("NOT-1").userId("USR-002")
                .message("m").category(NotifCategory.PTP).status(NotifStatus.UNREAD).build();
        when(repo.findById("NOT-1")).thenReturn(Optional.of(n));

        service().dismiss("USR-002", "NOT-1");

        // Dismiss clears it from history entirely.
        verify(repo).delete(n);
    }

    @Test
    void dismiss_otherUsersNotification_throwsAndDoesNotDelete() {
        Notification n = Notification.builder()
                .notificationId("NOT-9").userId("USR-999")
                .message("m").category(NotifCategory.PTP).status(NotifStatus.UNREAD).build();
        when(repo.findById("NOT-9")).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service().dismiss("USR-002", "NOT-9"))
                .isInstanceOf(UnauthorizedActionException.class);
        verify(repo, never()).delete(any(Notification.class));
    }

    @Test
    void unreadCount_delegatesToRepository() {
        when(repo.countByUserIdAndStatus("USR-002", NotifStatus.UNREAD)).thenReturn(3L);
        assertThat(service().unreadCount("USR-002")).isEqualTo(3L);
    }

    @Test
    void markAllRead_marksEveryUnreadAndReturnsCount() {
        Notification a = Notification.builder().notificationId("A").userId("USR-002")
                .status(NotifStatus.UNREAD).category(NotifCategory.PTP).build();
        Notification b = Notification.builder().notificationId("B").userId("USR-002")
                .status(NotifStatus.UNREAD).category(NotifCategory.LEGAL).build();
        when(repo.findByUserIdAndStatus("USR-002", NotifStatus.UNREAD)).thenReturn(List.of(a, b));

        long updated = service().markAllRead("USR-002");

        assertThat(updated).isEqualTo(2L);
        assertThat(a.getStatus()).isEqualTo(NotifStatus.READ);
        assertThat(b.getStatus()).isEqualTo(NotifStatus.READ);
        verify(repo).saveAll(List.of(a, b));
    }
}

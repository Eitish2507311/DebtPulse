package com.debtpulse.notification.service.impl;

import com.debtpulse.notification.exception.ResourceNotFoundException;
import com.debtpulse.notification.exception.UnauthorizedActionException;
import com.debtpulse.common.enums.NotifCategory;
import com.debtpulse.common.enums.NotifStatus;
import com.debtpulse.notification.dto.response.NotificationDto;
import com.debtpulse.notification.entity.Notification;
import com.debtpulse.notification.mapper.NotificationMapper;
import com.debtpulse.notification.repository.NotificationRepository;
import com.debtpulse.notification.repository.NotificationSpecifications;
import com.debtpulse.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository repo;
    private final NotificationMapper mapper;

    public NotificationServiceImpl(NotificationRepository repo, NotificationMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public NotificationDto create(String userId, String message, NotifCategory category) {
        Notification n = Notification.builder()
                .userId(userId)
                .message(message)
                .category(category)
                .status(NotifStatus.UNREAD)
                .createdDate(LocalDateTime.now())
                .build();
        Notification saved = repo.save(n);
        log.info("Notification created: id={} user={} category={}", saved.getNotificationId(),
                saved.getUserId(), saved.getCategory());
        return mapper.toDto(saved);
    }

    @Override
    public Page<NotificationDto> listForUser(String userId, NotifCategory category, NotifStatus status,
                                             LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime fromTs = from != null ? from.atStartOfDay() : null;
        LocalDateTime toTs = to != null ? to.atTime(LocalTime.MAX) : null;
        return repo.findAll(
                NotificationSpecifications.withFilters(userId, category, status, fromTs, toTs),
                pageable).map(mapper::toDto);
    }

    @Override
    public NotificationDto getById(String userId, String notificationId) {
        return mapper.toDto(findOwned(userId, notificationId));
    }

    @Override
    public long unreadCount(String userId) {
        return repo.countByUserIdAndStatus(userId, NotifStatus.UNREAD);
    }

    @Override
    public NotificationDto markRead(String userId, String notificationId) {
        Notification n = findOwned(userId, notificationId);
        n.setStatus(NotifStatus.READ);
        Notification saved = repo.save(n);
        log.info("Notification marked READ: id={} user={}", notificationId, userId);
        return mapper.toDto(saved);
    }

    @Override
    public void dismiss(String userId, String notificationId) {
        // Dismiss = remove from the user's history entirely (the dismiss action itself is audited
        // at the controller boundary, so there is still a trail of who cleared what).
        Notification n = findOwned(userId, notificationId);
        repo.delete(n);
        log.info("Notification dismissed (cleared) id={} user={}", notificationId, userId);
    }

    @Override
    public long markAllRead(String userId) {
        List<Notification> unread = repo.findByUserIdAndStatus(userId, NotifStatus.UNREAD);
        unread.forEach(n -> n.setStatus(NotifStatus.READ));
        repo.saveAll(unread);
        log.info("Marked {} notifications READ for user {}", unread.size(), userId);
        return unread.size();
    }

    /** Load a notification and confirm it belongs to the caller, else 404 / 403. */
    private Notification findOwned(String userId, String notificationId) {
        Notification n = repo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (userId == null || !userId.equals(n.getUserId())) {
            throw new UnauthorizedActionException("You cannot access another user's notification");
        }
        return n;
    }
}

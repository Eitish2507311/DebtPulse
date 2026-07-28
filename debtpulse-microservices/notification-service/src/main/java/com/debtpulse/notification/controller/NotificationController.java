package com.debtpulse.notification.controller;

import com.debtpulse.common.dto.PageResponse;
import com.debtpulse.common.enums.NotifCategory;
import com.debtpulse.common.enums.NotifStatus;
import com.debtpulse.common.security.AuthContext;
import com.debtpulse.notification.dto.response.NotificationDto;
import com.debtpulse.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Current-user notifications API (2.8). Every endpoint operates only on the authenticated
 * caller's own notifications, resolved via {@link AuthContext#currentUserId()}. Any logged-in
 * user may call these — there is no role restriction.
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "The current user's in-app notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List the current user's notifications (paginated, newest first, filterable)")
    public ResponseEntity<PageResponse<NotificationDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) NotifCategory category,
            @RequestParam(required = false) NotifStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return ResponseEntity.ok(PageResponse.of(
                notificationService.listForUser(AuthContext.currentUserId(), category, status,
                        from, to, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of the current user's notifications")
    public ResponseEntity<NotificationDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.getById(AuthContext.currentUserId(), id));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count the current user's unread notifications")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        long count = notificationService.unreadCount(AuthContext.currentUserId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<NotificationDto> markRead(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.markRead(AuthContext.currentUserId(), id));
    }

    @PatchMapping("/{id}/dismiss")
    @Operation(summary = "Dismiss a notification")
    public ResponseEntity<NotificationDto> dismiss(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.dismiss(AuthContext.currentUserId(), id));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all of the current user's unread notifications as read")
    public ResponseEntity<Map<String, Long>> markAllRead() {
        long updated = notificationService.markAllRead(AuthContext.currentUserId());
        return ResponseEntity.ok(Map.of("markedRead", updated));
    }
}

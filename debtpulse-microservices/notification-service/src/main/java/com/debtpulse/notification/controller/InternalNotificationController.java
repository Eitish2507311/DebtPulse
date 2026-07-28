package com.debtpulse.notification.controller;

import com.debtpulse.notification.dto.request.NotificationRequest;
import com.debtpulse.notification.dto.response.NotificationDto;
import com.debtpulse.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal notification-raising API. Every other microservice POSTs alerts here over Feign
 * ({@code POST /api/internal/notifications}) — this is the platform-wide notification sink.
 */
@RestController
@RequestMapping("/api/internal")
@Tag(name = "Internal - Notifications", description = "Service-to-service notification raising (Feign)")
public class InternalNotificationController {

    private final NotificationService notificationService;

    public InternalNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/notifications")
    public ResponseEntity<NotificationDto> create(@Valid @RequestBody NotificationRequest request) {
        NotificationDto created = notificationService.create(
                request.userId(), request.message(), request.category());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

package com.debtpulse.notification.controller;

import com.debtpulse.common.enums.NotifCategory;
import com.debtpulse.notification.dto.response.NotificationDto;
import com.debtpulse.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer test using standalone MockMvc with a mocked service. The controller reads
 * the caller from {@code AuthContext.currentUserId()}, so an authentication is placed in the
 * {@link SecurityContextHolder} in {@link #setUp()} and cleared afterwards.
 */
class NotificationControllerTest {

    private static final String USER = "USR-002";

    private MockMvc mockMvc;
    private NotificationService service;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = Mockito.mock(NotificationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(service)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private NotificationDto sample() {
        return new NotificationDto("NOT-1", USER, "Your PTP is due", "PTP", "UNREAD",
                LocalDateTime.of(2026, 7, 15, 9, 0));
    }

    @Test
    void list_returns200WithPagedContent() throws Exception {
        when(service.listForUser(eq(USER), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sample()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].notificationId").value("NOT-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void unreadCount_returnsCountMap() throws Exception {
        when(service.unreadCount(USER)).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    void markRead_returns200WithUpdatedDto() throws Exception {
        NotificationDto read = new NotificationDto("NOT-1", USER, "m", "PTP", "READ",
                LocalDateTime.of(2026, 7, 15, 9, 0));
        when(service.markRead(USER, "NOT-1")).thenReturn(read);

        mockMvc.perform(patch("/api/notifications/NOT-1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));
    }

    @Test
    void readAll_returnsMarkedCount() throws Exception {
        when(service.markAllRead(USER)).thenReturn(4L);

        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedRead").value(4));
    }
}

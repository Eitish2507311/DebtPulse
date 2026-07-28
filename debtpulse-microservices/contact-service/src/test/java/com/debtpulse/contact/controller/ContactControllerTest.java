package com.debtpulse.contact.controller;

import com.debtpulse.contact.dto.response.ContactAttemptDto;
import com.debtpulse.contact.service.ContactService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer test using standalone MockMvc (no security context) so the mapping,
 * validation and JSON contract are exercised in isolation with a mocked service.
 */
class ContactControllerTest {

    private MockMvc mockMvc;
    private ContactService contactService;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        contactService = Mockito.mock(ContactService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ContactController(contactService)).build();
    }

    private ContactAttemptDto sample() {
        return new ContactAttemptDto("CT-1", "ACC-1", "USR-002",
                LocalDateTime.now(), "CALL", "CONNECTED", "spoke to borrower", "LOGGED", LocalDateTime.now());
    }

    @Test
    void create_returns201() throws Exception {
        when(contactService.create(any())).thenReturn(sample());

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "accountId", "ACC-1",
                                "channel", "CALL",
                                "outcome", "CONNECTED",
                                "notes", "spoke to borrower"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contactId").value("CT-1"))
                .andExpect(jsonPath("$.outcome").value("CONNECTED"));
    }

    @Test
    void create_missingAccountId_returns400() throws Exception {
        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "channel", "CALL",
                                "outcome", "CONNECTED"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returns200() throws Exception {
        when(contactService.getById(eq("CT-1"))).thenReturn(sample());

        mockMvc.perform(get("/api/contacts/CT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-1"))
                .andExpect(jsonPath("$.status").value("LOGGED"));
    }
}

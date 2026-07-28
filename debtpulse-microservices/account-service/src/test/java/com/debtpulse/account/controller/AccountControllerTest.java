package com.debtpulse.account.controller;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.DpdBucket;
import com.debtpulse.account.entity.DelinquentAccount;
import com.debtpulse.account.feign.AuthClient;
import com.debtpulse.account.mapper.AccountMapper;
import com.debtpulse.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
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
class AccountControllerTest {

    private MockMvc mockMvc;
    private AccountService accountService;
    private AuthClient authClient;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        accountService = Mockito.mock(AccountService.class);
        authClient = Mockito.mock(AuthClient.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AccountController(accountService, new AccountMapper(), authClient))
                .build();
    }

    @Test
    void getById_returns200() throws Exception {
        DelinquentAccount account = DelinquentAccount.builder()
                .accountId("ACC-1").loanRef("LN-1").borrowerName("Asha")
                .bucket(DpdBucket.X60).status(AccountStatus.ACTIVE).build();
        when(accountService.getById(eq("ACC-1"))).thenReturn(account);

        mockMvc.perform(get("/api/accounts/{id}", "ACC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-1"))
                .andExpect(jsonPath("$.bucket").value("X60"));
    }

    @Test
    void create_returns201() throws Exception {
        DelinquentAccount saved = DelinquentAccount.builder()
                .accountId("ACC-9").loanRef("LN-9").borrowerName("Ben")
                .bucket(DpdBucket.X30).status(AccountStatus.ACTIVE).build();
        when(accountService.importAccount(any(DelinquentAccount.class), any())).thenReturn(saved);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "loanRef", "LN-9",
                                "borrowerName", "Ben",
                                "principalAmount", new BigDecimal("50000"),
                                "totalOverdue", new BigDecimal("5000"),
                                "dpd", 20))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value("ACC-9"));
    }
}

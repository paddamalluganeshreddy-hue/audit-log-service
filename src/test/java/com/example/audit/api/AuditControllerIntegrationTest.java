package com.example.audit.api;

import com.example.audit.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AuditControllerIntegrationTest {
    private MockMvc mvc;

    @BeforeEach void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mvc = standaloneSetup(new AuditController(new AuditLogService(mapper, 365, "token,ssn"))).build();
    }

    @Test void writesQueriesRedactsAndVerifies() throws Exception {
        String event = """
                {"eventType":"ACCOUNT_ACCESS","actorId":"analyst-7","resourceType":"CLIENT_ACCOUNT","resourceId":"acct-42","payload":{"purpose":"support","token":"secret-value"}}
                """;
        mvc.perform(post("/audit/write").contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sequence").value(1))
                .andExpect(jsonPath("$.selfHash").isNotEmpty());
        mvc.perform(get("/audit/query").param("resourceId", "acct-42"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.records[0].payload.token").value("[REDACTED]"));
        mvc.perform(get("/audit/verify"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.intact").value(true));
        mvc.perform(get("/audit/report").param("accountId", "acct-42"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }

    @Test void rejectsInvalidWritePayload() throws Exception {
        mvc.perform(post("/audit/write").contentType(MediaType.APPLICATION_JSON).content("{\"eventType\":\"READ\"}"))
                .andExpect(status().isBadRequest());
    }
}

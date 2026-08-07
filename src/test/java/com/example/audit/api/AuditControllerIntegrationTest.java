package com.example.audit.api;

import com.example.audit.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.hamcrest.Matchers.startsWith;

class AuditControllerIntegrationTest {
    private MockMvc mvc;

    @BeforeEach void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mvc = standaloneSetup(new AuditController(new AuditLogService(mapper, Clock.systemUTC(), 365, "token,ssn", "test-redaction-key", 65536)))
                .addFilters(new AuditCacheControlFilter())
                .setControllerAdvice(new ApiExceptionHandler()).build();
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
                .andExpect(jsonPath("$.records[0].payload.token").value(startsWith("[REDACTED:")));
        mvc.perform(get("/audit/verify"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.intact").value(true))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"));
        mvc.perform(get("/audit/report").param("accountId", "acct-42"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }

    @Test void rejectsInvalidWritePayload() throws Exception {
        mvc.perform(post("/audit/write").contentType(MediaType.APPLICATION_JSON).content("{\"eventType\":\"READ\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.actorId").value("must not be blank"));
    }

    @Test void rejectsOversizedAuditMetadata() throws Exception {
        String oversizedActor = "a".repeat(129);
        String event = "{\"eventType\":\"READ\",\"actorId\":\"" + oversizedActor + "\",\"resourceType\":\"DOCUMENT\",\"resourceId\":\"doc-1\",\"payload\":{}}";
        mvc.perform(post("/audit/write").contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.actorId").value("size must be between 0 and 128"));
    }

    @Test void rejectsOversizedPayload() throws Exception {
        String event = "{\"eventType\":\"READ\",\"actorId\":\"user-1\",\"resourceType\":\"DOCUMENT\",\"resourceId\":\"doc-1\",\"payload\":{\"notes\":\"" + "x".repeat(65536) + "\"}}";
        mvc.perform(post("/audit/write").contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("payload exceeds the configured size limit"));
    }

    @Test void rejectsAnInvalidTimeRange() throws Exception {
        mvc.perform(get("/audit/query").param("from", "2026-02-01T00:00:00Z").param("to", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("from must not be after to"));
    }

    @Test void rejectsBlankComplianceAccountId() throws Exception {
        mvc.perform(get("/audit/report").param("accountId", " "))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("accountId must not be blank"));
    }

    @Test void exportsSelectedRecordsWithVerificationMetadata() throws Exception {
        String event = """
                {"eventType":"READ","actorId":"user-9","resourceType":"DOCUMENT","resourceId":"doc-8","payload":{"ssn":"123-45-6789"}}
                """;
        mvc.perform(post("/audit/write").contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isCreated());
        mvc.perform(get("/audit/export").param("actorId", "user-9"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.algorithm").value("SHA-256"))
                .andExpect(jsonPath("$.recordCount").value(1))
                .andExpect(jsonPath("$.fullChainVerification.intact").value(true))
                .andExpect(jsonPath("$.lastRecordSelfHash").isNotEmpty())
                .andExpect(jsonPath("$.records[0].payload.ssn").value(startsWith("[REDACTED:")));
    }

    @Test void rejectsUnfilteredExport() throws Exception {
        mvc.perform(get("/audit/export"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("actorId or resourceId is required for export"));
    }
}

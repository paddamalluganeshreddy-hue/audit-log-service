package com.example.audit.service;

import com.example.audit.domain.AuditRecord;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HashChainTest {
    private final ObjectMapper mapper = new ObjectMapper();
    @Test void verifiesAnUntamperedChain() throws Exception {
        var payload = mapper.readTree("{\"action\":\"read\"}");
        Instant time = Instant.parse("2026-01-01T00:00:00Z");
        String firstHash = HashChain.compute(1, "READ", "user-1", "DOCUMENT", "doc-1", payload, time.toString(), null, mapper);
        AuditRecord first = new AuditRecord(1, "READ", "user-1", "DOCUMENT", "doc-1", payload, time, firstHash, null);
        assertTrue(HashChain.verify(List.of(first), mapper).intact());
    }
    @Test void detectsPayloadTampering() throws Exception {
        var original = mapper.readTree("{\"action\":\"read\"}"); var altered = mapper.readTree("{\"action\":\"delete\"}");
        Instant time = Instant.parse("2026-01-01T00:00:00Z");
        String hash = HashChain.compute(1, "READ", "user-1", "DOCUMENT", "doc-1", original, time.toString(), null, mapper);
        var corrupted = new AuditRecord(1, "READ", "user-1", "DOCUMENT", "doc-1", altered, time, hash, null);
        var result = HashChain.verify(List.of(corrupted), mapper);
        assertFalse(result.intact()); assertEquals("SELF_HASH_MISMATCH", result.violationType());
    }
    @Test void detectsBrokenPredecessorLinkAtTheAffectedRecord() throws Exception {
        var payload = mapper.readTree("{}"); Instant time = Instant.parse("2026-01-01T00:00:00Z");
        String firstHash = HashChain.compute(1, "READ", "user-1", "DOCUMENT", "doc-1", payload, time.toString(), null, mapper);
        AuditRecord first = new AuditRecord(1, "READ", "user-1", "DOCUMENT", "doc-1", payload, time, firstHash, null);
        String secondHash = HashChain.compute(2, "READ", "user-1", "DOCUMENT", "doc-2", payload, time.toString(), "wrong", mapper);
        AuditRecord brokenSecond = new AuditRecord(2, "READ", "user-1", "DOCUMENT", "doc-2", payload, time, secondHash, "wrong");
        var result = HashChain.verify(List.of(first, brokenSecond), mapper);
        assertFalse(result.intact()); assertEquals(2, result.checkedRecords());
        assertEquals(2, result.firstInconsistentSequence()); assertEquals("PREVIOUS_HASH_MISMATCH", result.violationType());
    }
}

package com.example.audit.service;

import com.example.audit.api.AuditWriteRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class RetentionIntegrityTest {
    @Test void archivesExpiredRecordsWithoutBreakingTheChain() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AuditLogService service = new AuditLogService(new ObjectMapper(), clock, 30, "secret", "test-redaction-key", 65536);
        service.append(new AuditWriteRequest("READ", "user-1", "DOCUMENT", "doc-1", new ObjectMapper().readTree("{}")));
        clock.advance(Duration.ofDays(31));
        assertEquals(1, service.archiveExpired());
        assertTrue(service.verify().intact());
        assertEquals(1, service.query(null, null, null, null, null, null, 0, 10).size());
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;
        private MutableClock(Instant start) { instant = new AtomicReference<>(start); }
        void advance(Duration duration) { instant.updateAndGet(value -> value.plus(duration)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant.get(); }
    }
}

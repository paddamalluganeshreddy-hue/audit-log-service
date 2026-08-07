package com.example.audit;

import com.example.audit.api.AuditWriteRequest;
import com.example.audit.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class H2PersistenceIntegrationTest {
    @Autowired AuditLogService service;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper mapper;

    @Test void persistsAuditRecordsInTheH2Table() throws Exception {
        service.append(new AuditWriteRequest("READ", "h2-test", "DOCUMENT", "h2-doc", mapper.readTree("{}")));
        Integer count = jdbcTemplate.queryForObject("select count(*) from audit_records where actor_id = ?", Integer.class, "h2-test");
        assertEquals(1, count);
    }
}

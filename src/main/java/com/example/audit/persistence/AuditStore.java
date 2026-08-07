package com.example.audit.persistence;

import com.example.audit.domain.AuditRecord;
import java.time.Instant;
import java.util.List;

public interface AuditStore {
    List<AuditRecord> findAll();
    AuditRecord save(AuditRecord record);
    int archiveBefore(Instant threshold);
}

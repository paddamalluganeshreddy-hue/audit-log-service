package com.example.audit.persistence;

import com.example.audit.domain.AuditRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class InMemoryAuditStore implements AuditStore {
    private final List<AuditRecord> records = new ArrayList<>();
    @Override public List<AuditRecord> findAll() { return List.copyOf(records); }
    @Override public AuditRecord save(AuditRecord record) { records.add(record); return record; }
    @Override public int archiveBefore(Instant threshold) { return (int) records.stream().filter(record -> record.timestamp().isBefore(threshold)).count(); }
}

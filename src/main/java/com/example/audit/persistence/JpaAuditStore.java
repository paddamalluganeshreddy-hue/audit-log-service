package com.example.audit.persistence;

import com.example.audit.domain.AuditRecord;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
public class JpaAuditStore implements AuditStore {
    private final AuditRecordJpaRepository repository;
    private final ObjectMapper mapper;
    public JpaAuditStore(AuditRecordJpaRepository repository, ObjectMapper mapper) { this.repository = repository; this.mapper = mapper; }
    @Override public List<AuditRecord> findAll() { return repository.findAllByOrderBySequenceAsc().stream().map(this::toRecord).toList(); }
    @Override public AuditRecord save(AuditRecord record) {
        repository.save(new AuditRecordEntity(record.sequence(), record.eventType(), record.actorId(), record.resourceType(), record.resourceId(), record.payload().toString(), record.timestamp(), record.selfHash(), record.prevHash(), false));
        return record;
    }
    @Override public int archiveBefore(Instant threshold) {
        List<AuditRecordEntity> expired = repository.findAllByArchivedFalseAndTimestampBefore(threshold);
        expired.forEach(AuditRecordEntity::archive); repository.saveAll(expired); return expired.size();
    }
    private AuditRecord toRecord(AuditRecordEntity entity) {
        try { return new AuditRecord(entity.getSequence(), entity.getEventType(), entity.getActorId(), entity.getResourceType(), entity.getResourceId(), mapper.readTree(entity.getPayload()), entity.getTimestamp(), entity.getSelfHash(), entity.getPrevHash()); }
        catch (JacksonException exception) { throw new IllegalStateException("Stored audit payload is invalid", exception); }
    }
}

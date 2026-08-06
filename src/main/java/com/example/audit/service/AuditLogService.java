package com.example.audit.service;

import com.example.audit.api.AuditWriteRequest;
import com.example.audit.domain.AuditRecord;
import com.example.audit.domain.ChainVerification;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@Service
public class AuditLogService {
    private final List<AuditRecord> active = new ArrayList<>();
    private final List<AuditRecord> archive = new ArrayList<>();
    private final ReentrantLock writeLock = new ReentrantLock();
    private final ObjectMapper mapper; private final Clock clock = Clock.systemUTC();
    private final int retentionDays; private final Set<String> sensitiveFields;
    public AuditLogService(ObjectMapper mapper, @Value("${audit.retention-days:365}") int retentionDays,
                           @Value("${audit.sensitive-payload-fields:password,ssn,token,secret}") String fields) {
        this.mapper = mapper; this.retentionDays = retentionDays;
        this.sensitiveFields = Arrays.stream(fields.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public AuditRecord append(AuditWriteRequest request) {
        writeLock.lock();
        try {
            long sequence = active.size() + archive.size() + 1L;
            String previous = active.isEmpty() ? (archive.isEmpty() ? null : archive.getLast().selfHash()) : active.getLast().selfHash();
            Instant now = clock.instant(); JsonNode payload = request.payload().deepCopy();
            String hash = HashChain.compute(sequence, request.eventType(), request.actorId(), request.resourceType(), request.resourceId(), payload, now.toString(), previous, mapper);
            AuditRecord record = new AuditRecord(sequence, request.eventType(), request.actorId(), request.resourceType(), request.resourceId(), payload, now, hash, previous);
            active.add(record); return record;
        } finally { writeLock.unlock(); }
    }
    public List<AuditRecord> query(String actorId, String resourceType, String resourceId, String eventType, Instant from, Instant to, int page, int size) {
        return filtered(actorId, resourceType, resourceId, eventType, from, to).skip((long) page * size).limit(size).map(this::redacted).toList();
    }
    public long count(String actorId, String resourceType, String resourceId, String eventType, Instant from, Instant to) { return filtered(actorId, resourceType, resourceId, eventType, from, to).count(); }
    public ChainVerification verify() { return HashChain.verify(allRecords(), mapper); }
    public List<AuditRecord> export(String actorId, String resourceId) { return filtered(actorId, null, resourceId, null, null, null).map(this::redacted).toList(); }
    public int archiveExpired() {
        writeLock.lock(); try { Instant threshold = clock.instant().minus(retentionDays, ChronoUnit.DAYS); List<AuditRecord> expired = active.stream().filter(r -> r.timestamp().isBefore(threshold)).toList(); archive.addAll(expired); active.removeAll(expired); return expired.size(); } finally { writeLock.unlock(); }
    }
    private Stream<AuditRecord> filtered(String actorId, String resourceType, String resourceId, String eventType, Instant from, Instant to) {
        return allRecords().stream().filter(r -> actorId == null || actorId.equals(r.actorId())).filter(r -> resourceType == null || resourceType.equals(r.resourceType())).filter(r -> resourceId == null || resourceId.equals(r.resourceId())).filter(r -> eventType == null || eventType.equals(r.eventType())).filter(r -> from == null || !r.timestamp().isBefore(from)).filter(r -> to == null || !r.timestamp().isAfter(to));
    }
    private List<AuditRecord> allRecords() { return Stream.concat(archive.stream(), active.stream()).sorted(Comparator.comparingLong(AuditRecord::sequence)).toList(); }
    private AuditRecord redacted(AuditRecord source) {
        JsonNode payload = source.payload().deepCopy(); if (payload.isObject()) sensitiveFields.forEach(field -> { if (payload.has(field)) ((tools.jackson.databind.node.ObjectNode) payload).put(field, "[REDACTED]"); });
        return new AuditRecord(source.sequence(), source.eventType(), source.actorId(), source.resourceType(), source.resourceId(), payload, source.timestamp(), source.selfHash(), source.prevHash());
    }
}

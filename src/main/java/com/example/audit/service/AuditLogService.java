package com.example.audit.service;

import com.example.audit.api.AuditWriteRequest;
import com.example.audit.domain.AuditRecord;
import com.example.audit.domain.ChainVerification;
import com.example.audit.persistence.AuditStore;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@Service
public class AuditLogService {
    private final ReentrantLock writeLock = new ReentrantLock();
    private final ObjectMapper mapper; private final Clock clock; private final AuditStore store;
    private final int retentionDays; private final Set<String> sensitiveFields; private final String redactionKey; private final int maxPayloadBytes;
    @Autowired
    public AuditLogService(ObjectMapper mapper, Clock clock, AuditStore store, @Value("${audit.retention-days:365}") int retentionDays,
                           @Value("${audit.sensitive-payload-fields:password,ssn,token,secret}") String fields,
                           @Value("${audit.redaction-key:development-only-change-me}") String redactionKey,
                           @Value("${audit.max-payload-bytes:65536}") int maxPayloadBytes) {
        this.mapper = mapper; this.clock = clock; this.store = store; this.retentionDays = retentionDays; this.redactionKey = redactionKey; this.maxPayloadBytes = maxPayloadBytes;
        this.sensitiveFields = Arrays.stream(fields.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    @Transactional
    public AuditRecord append(AuditWriteRequest request) {
        writeLock.lock();
        try {
            List<AuditRecord> existing = store.findAll();
            long sequence = existing.isEmpty() ? 1L : existing.getLast().sequence() + 1L;
            String previous = existing.isEmpty() ? null : existing.getLast().selfHash();
            enforcePayloadSize(request.payload());
            Instant now = clock.instant(); JsonNode payload = sanitizeForStorage(request.payload());
            String hash = HashChain.compute(sequence, request.eventType(), request.actorId(), request.resourceType(), request.resourceId(), payload, now.toString(), previous, mapper);
            AuditRecord record = new AuditRecord(sequence, request.eventType(), request.actorId(), request.resourceType(), request.resourceId(), payload, now, hash, previous);
            return store.save(record);
        } finally { writeLock.unlock(); }
    }
    public List<AuditRecord> query(String actorId, String resourceType, String resourceId, String eventType, Instant from, Instant to, int page, int size) {
        return filtered(actorId, resourceType, resourceId, eventType, from, to).skip((long) page * size).limit(size).map(this::redacted).toList();
    }
    public long count(String actorId, String resourceType, String resourceId, String eventType, Instant from, Instant to) { return filtered(actorId, resourceType, resourceId, eventType, from, to).count(); }
    public ChainVerification verify() { return HashChain.verify(allRecords(), mapper); }
    public List<AuditRecord> export(String actorId, String resourceId) { return filtered(actorId, null, resourceId, null, null, null).map(this::redacted).toList(); }
    @Transactional
    public int archiveExpired() {
        writeLock.lock(); try { return store.archiveBefore(clock.instant().minus(retentionDays, ChronoUnit.DAYS)); } finally { writeLock.unlock(); }
    }
    private Stream<AuditRecord> filtered(String actorId, String resourceType, String resourceId, String eventType, Instant from, Instant to) {
        return allRecords().stream().filter(r -> actorId == null || actorId.equals(r.actorId())).filter(r -> resourceType == null || resourceType.equals(r.resourceType())).filter(r -> resourceId == null || resourceId.equals(r.resourceId())).filter(r -> eventType == null || eventType.equals(r.eventType())).filter(r -> from == null || !r.timestamp().isBefore(from)).filter(r -> to == null || !r.timestamp().isAfter(to));
    }
    private List<AuditRecord> allRecords() { return store.findAll(); }
    private AuditRecord redacted(AuditRecord source) {
        return new AuditRecord(source.sequence(), source.eventType(), source.actorId(), source.resourceType(), source.resourceId(), source.payload().deepCopy(), source.timestamp(), source.selfHash(), source.prevHash());
    }
    private JsonNode sanitizeForStorage(JsonNode input) {
        JsonNode payload = input.deepCopy();
        if (payload.isObject()) sensitiveFields.forEach(field -> { if (payload.has(field)) ((tools.jackson.databind.node.ObjectNode) payload).put(field, "[REDACTED:" + hmac(payload.get(field).toString()) + "]"); });
        return payload;
    }
    private void enforcePayloadSize(JsonNode payload) {
        if (payload.toString().getBytes(StandardCharsets.UTF_8).length > maxPayloadBytes)
            throw new IllegalArgumentException("payload exceeds the configured size limit");
    }
    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(redactionKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (java.security.GeneralSecurityException exception) { throw new IllegalStateException("Cannot create redaction commitment", exception); }
    }
}

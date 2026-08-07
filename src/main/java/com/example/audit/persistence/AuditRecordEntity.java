package com.example.audit.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_records")
public class AuditRecordEntity {
    @Id private long sequence;
    @Column(nullable = false, length = 128) private String eventType;
    @Column(nullable = false, length = 128) private String actorId;
    @Column(nullable = false, length = 128) private String resourceType;
    @Column(nullable = false, length = 256) private String resourceId;
    @Lob @Column(nullable = false) private String payload;
    @Column(nullable = false) private Instant timestamp;
    @Column(nullable = false, length = 64) private String selfHash;
    @Column(length = 64) private String prevHash;
    @Column(nullable = false) private boolean archived;

    protected AuditRecordEntity() { }
    AuditRecordEntity(long sequence, String eventType, String actorId, String resourceType, String resourceId, String payload, Instant timestamp, String selfHash, String prevHash, boolean archived) {
        this.sequence = sequence; this.eventType = eventType; this.actorId = actorId; this.resourceType = resourceType; this.resourceId = resourceId; this.payload = payload; this.timestamp = timestamp; this.selfHash = selfHash; this.prevHash = prevHash; this.archived = archived;
    }
    public long getSequence() { return sequence; } public String getEventType() { return eventType; } public String getActorId() { return actorId; }
    public String getResourceType() { return resourceType; } public String getResourceId() { return resourceId; } public String getPayload() { return payload; }
    public Instant getTimestamp() { return timestamp; } public String getSelfHash() { return selfHash; } public String getPrevHash() { return prevHash; }
    public boolean isArchived() { return archived; } public void archive() { archived = true; }
}

package com.example.audit.domain;

import tools.jackson.databind.JsonNode;
import java.time.Instant;

/** Immutable, server-created event. selfHash commits to every field except itself. */
public record AuditRecord(
        long sequence,
        String eventType,
        String actorId,
        String resourceType,
        String resourceId,
        JsonNode payload,
        Instant timestamp,
        String selfHash,
        String prevHash) { }

package com.example.audit.service;

import com.example.audit.domain.AuditRecord;
import com.example.audit.domain.ChainVerification;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/** SHA-256 over canonical JSON makes field ordering irrelevant to the chain commitment. */
public final class HashChain {
    private HashChain() { }
    public static String compute(long sequence, String eventType, String actorId, String resourceType,
                                 String resourceId, JsonNode payload, String timestamp, String prevHash, ObjectMapper mapper) {
        try {
            var node = mapper.createObjectNode();
            node.put("sequence", sequence); node.put("eventType", eventType); node.put("actorId", actorId);
            node.put("resourceType", resourceType); node.put("resourceId", resourceId); node.set("payload", payload);
            node.put("timestamp", timestamp); node.put("prevHash", prevHash == null ? "" : prevHash);
            return hex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(node)));
        } catch (NoSuchAlgorithmException | JacksonException exception) { throw new IllegalStateException("Cannot create audit hash", exception); }
    }
    public static ChainVerification verify(List<AuditRecord> records, ObjectMapper mapper) {
        String expectedPrevious = null;
        for (int index = 0; index < records.size(); index++) {
            AuditRecord record = records.get(index);
            if (!java.util.Objects.equals(expectedPrevious, record.prevHash()))
                return ChainVerification.broken(index, record.sequence(), "PREVIOUS_HASH_MISMATCH", "prevHash does not link to preceding record");
            String expectedSelf = compute(record.sequence(), record.eventType(), record.actorId(), record.resourceType(), record.resourceId(), record.payload(), record.timestamp().toString(), record.prevHash(), mapper);
            if (!expectedSelf.equals(record.selfHash()))
                return ChainVerification.broken(index + 1L, record.sequence(), "SELF_HASH_MISMATCH", "record contents do not match selfHash");
            expectedPrevious = record.selfHash();
        }
        return ChainVerification.intact(records.size());
    }
    private static String hex(byte[] bytes) { return java.util.HexFormat.of().formatHex(bytes); }
}

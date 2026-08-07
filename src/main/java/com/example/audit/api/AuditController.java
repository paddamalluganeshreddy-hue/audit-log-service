package com.example.audit.api;

import com.example.audit.domain.AuditRecord;
import com.example.audit.service.AuditLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/audit") @org.springframework.validation.annotation.Validated
public class AuditController {
    private final AuditLogService service;
    public AuditController(AuditLogService service) { this.service = service; }
    @PostMapping("/write") @ResponseStatus(HttpStatus.CREATED) public AuditRecord write(@Valid @RequestBody AuditWriteRequest request) { return service.append(request); }
    @GetMapping("/query") public Map<String, Object> query(@RequestParam(required = false) String actorId, @RequestParam(required = false) String resourceType, @RequestParam(required = false) String resourceId, @RequestParam(required = false) String eventType, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > 200) throw new IllegalArgumentException("page must be >= 0 and size must be 1..200");
        validateTimeRange(from, to);
        List<AuditRecord> records = service.query(actorId, resourceType, resourceId, eventType, from, to, page, size); return Map.of("records", records, "page", page, "size", size, "total", service.count(actorId, resourceType, resourceId, eventType, from, to));
    }
    @GetMapping("/verify") public Object verify() { return service.verify(); }
    @PostMapping("/retention/archive") public Map<String, Integer> archiveExpired() { return Map.of("archived", service.archiveExpired()); }
    @GetMapping("/export") public AuditExport export(@RequestParam(required = false) String actorId, @RequestParam(required = false) String resourceId) {
        List<AuditRecord> records = service.export(actorId, resourceId);
        String firstPreviousHash = records.isEmpty() ? null : records.getFirst().prevHash();
        String lastSelfHash = records.isEmpty() ? null : records.getLast().selfHash();
        return new AuditExport("SHA-256", records, records.size(), firstPreviousHash, lastSelfHash, service.verify(),
                "Each record selfHash can be recomputed independently. firstRecordPreviousHash and lastRecordSelfHash anchor this filtered export in the full service chain; fullChainVerification verifies the source chain at export time.");
    }
    @GetMapping("/report") public Map<String, Object> report(@RequestParam @NotBlank String accountId, @RequestParam(required = false) String actorId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) { validateTimeRange(from, to); List<AuditRecord> records = service.query(actorId, "CLIENT_ACCOUNT", accountId, "ACCOUNT_ACCESS", from, to, 0, 200); return Map.of("accountId", accountId, "eventType", "ACCOUNT_ACCESS", "records", records, "total", service.count(actorId, "CLIENT_ACCOUNT", accountId, "ACCOUNT_ACCESS", from, to)); }
    private static void validateTimeRange(Instant from, Instant to) { if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("from must not be after to"); }
}

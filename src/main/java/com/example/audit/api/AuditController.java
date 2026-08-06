package com.example.audit.api;

import com.example.audit.domain.AuditRecord;
import com.example.audit.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/audit")
public class AuditController {
    private final AuditLogService service;
    public AuditController(AuditLogService service) { this.service = service; }
    @PostMapping("/write") @ResponseStatus(HttpStatus.CREATED) public AuditRecord write(@Valid @RequestBody AuditWriteRequest request) { return service.append(request); }
    @GetMapping("/query") public Map<String, Object> query(@RequestParam(required = false) String actorId, @RequestParam(required = false) String resourceType, @RequestParam(required = false) String resourceId, @RequestParam(required = false) String eventType, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > 200) throw new IllegalArgumentException("page must be >= 0 and size must be 1..200");
        List<AuditRecord> records = service.query(actorId, resourceType, resourceId, eventType, from, to, page, size); return Map.of("records", records, "page", page, "size", size, "total", service.count(actorId, resourceType, resourceId, eventType, from, to));
    }
    @GetMapping("/verify") public Object verify() { return service.verify(); }
    @PostMapping("/retention/archive") public Map<String, Integer> archiveExpired() { return Map.of("archived", service.archiveExpired()); }
    @GetMapping("/export") public Map<String, Object> export(@RequestParam(required = false) String actorId, @RequestParam(required = false) String resourceId) { List<AuditRecord> records = service.export(actorId, resourceId); return Map.of("algorithm", "SHA-256", "records", records, "recordCount", records.size(), "verificationScope", "Export hashes link to the full service chain; verify against included boundary hashes or the full chain."); }
    @GetMapping("/report") public Map<String, Object> report(@RequestParam String accountId, @RequestParam(required = false) String actorId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) { List<AuditRecord> records = service.query(actorId, "CLIENT_ACCOUNT", accountId, "ACCOUNT_ACCESS", from, to, 0, 200); return Map.of("accountId", accountId, "eventType", "ACCOUNT_ACCESS", "records", records, "total", service.count(actorId, "CLIENT_ACCOUNT", accountId, "ACCOUNT_ACCESS", from, to)); }
}

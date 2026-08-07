package com.example.audit.api;

import com.example.audit.domain.AuditRecord;
import com.example.audit.domain.ChainVerification;
import java.util.List;

/** Export bundle with enough metadata to validate selected record commitments and their chain boundaries. */
public record AuditExport(String algorithm, List<AuditRecord> records, long recordCount,
                          String firstRecordPreviousHash, String lastRecordSelfHash,
                          ChainVerification fullChainVerification, String verificationScope) { }

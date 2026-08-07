# API and compliance assumptions

## Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/audit/write` | Append an event and return its hashes. |
| GET | `/audit/query` | Filter and page records. Maximum page size is 200. |
| GET | `/audit/verify` | Verify the complete active and archived chain. |
| POST | `/audit/retention/archive` | Archive records beyond the configured retention window. |
| GET | `/audit/export` | Return selected records and chain-verification metadata. |
| GET | `/audit/report` | Return `ACCOUNT_ACCESS` events for a client account. |

Write-request field limits: `eventType`, `actorId`, and `resourceType` are limited to 128 characters; `resourceId` is limited to 256 characters.

## Compliance-report scope

“Audit of client account access” is interpreted as events written with `eventType=ACCOUNT_ACCESS`, `resourceType=CLIENT_ACCOUNT`, and `resourceId` equal to the supplied `accountId`. The report additionally accepts actor and time filters. Ambiguities to resolve with a regulator include the official account identifier, required access event taxonomy, authorization model, retention jurisdiction, time zone, export format/signing, and whether redacted fields may be disclosed.

Bulk export requires at least one of `actorId` or `resourceId`; unfiltered full-log exports are rejected.

Sensitive fields are replaced during ingestion with keyed redaction commitments, so exports retain hashes that can be recomputed against their returned payloads. `firstRecordPreviousHash` and `lastRecordSelfHash` bound the filtered data against the source chain, while `fullChainVerification` records the source-chain result at export time. A filtered export does not disclose unselected adjacent events, so it needs the included boundary hashes and a trusted chain checkpoint for complete provenance.

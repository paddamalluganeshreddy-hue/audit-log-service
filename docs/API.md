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

## Compliance-report scope

“Audit of client account access” is interpreted as events written with `eventType=ACCOUNT_ACCESS`, `resourceType=CLIENT_ACCOUNT`, and `resourceId` equal to the supplied `accountId`. The report additionally accepts actor and time filters. Ambiguities to resolve with a regulator include the official account identifier, required access event taxonomy, authorization model, retention jurisdiction, time zone, export format/signing, and whether redacted fields may be disclosed.

Exports retain original hashes while returning redacted payload views. They can establish record content commitments, but a filtered export may not contain all adjacent chain members; use the exported boundary hashes together with a full-chain verification report for an independent review.

# AI usage log

This log records AI-assisted work performed in this repository. The submitting engineer remains responsible for reviewing the implementation, understanding the design, running validation, and deciding whether changes are suitable for submission or deployment.

| Stage | AI contribution | Engineer decision / outcome | Validation or commit |
|---|---|---|---|
| Initial setup | Generated the Java 21 / Spring Boot 4 Maven structure, base application, README, attestation, and documentation layout. | Accepted for the assignment prototype. | Included in initial `milestone` commit. |
| Core audit model | Proposed immutable `AuditRecord` fields, server-assigned UTC timestamps, SHA-256 chaining, and verification result types. | Accepted. Server time was selected instead of client timestamps. | Included in `milestone`; hash-chain unit tests added. |
| Write, query, and verify APIs | Generated append-only write, filters/pagination, and verification routes. | Accepted with page-size bounds and explicit error responses. | Included in `milestone`; later validation refinements in `439bf79`. |
| Retention and compliance | Proposed archive-aware verification, configured sensitive fields, account-access reporting assumptions, and supporting documentation. | Modified: archives remain part of verification; report scope is limited to `ACCOUNT_ACCESS` on `CLIENT_ACCOUNT`. | Included in `milestone`; retention proof added in `8442089`. |
| Export metadata | Identified that a filtered export needed boundary metadata and a current chain result. | Accepted. Export now includes boundary hashes and full-chain verification status. | `6c0e438`, focused tests passed. |
| Input validation | Added invalid-time-range and blank-account handling. | Modified after a test exposed that standalone method validation was insufficient; explicit boundary validation was retained. | `439bf79`, `6d9aa1c`; focused tests passed. |
| Testability and retention proof | Recommended an injectable `Clock` to test retention deterministically. | Accepted. The production clock is UTC; tests use a mutable clock. | `8442089`, focused tests passed. |
| Verification diagnostics | Found that a predecessor-link mismatch underreported the number of inspected records. | Accepted. The result now reports the affected record as inspected. | `837cabd`, focused tests passed. |
| Continuous integration | Generated a GitHub Actions workflow for Java 21 and Maven verification. | Accepted. Workflow runs on pushes and pull requests to `main`. | `98fbcaf`; local `mvn verify` passed. |
| Redaction redesign | Found that response-only redaction prevented recomputation of a returned `selfHash`. Proposed ingest-time keyed HMAC commitments instead. | Accepted. Sensitive top-level configured fields are replaced before storage and hash chaining; plaintext is not retained by this prototype. | `1ed2506`; full `mvn verify` passed. |
| AI-log consolidation | Consolidated prior AI assistance, accepted changes, and revisions into this file. | Accepted. Future AI-assisted stages will be appended here before commit. | `aabf816`. |
| Validation error contract | Proposed a consistent JSON response containing a general error and field-specific validation messages for invalid write requests. | Accepted to make API-client error handling predictable. | HTTP integration test passed. |
| Export scope guard | Identified that an unfiltered export could disclose the entire audit log. Proposed requiring an actor or resource filter. | Accepted to reduce accidental broad data disclosure. | HTTP integration test passed. |
| Metadata bounds | Proposed maximum lengths for event and identifier fields to avoid oversized chain metadata. | Accepted with 128-character event/actor/type limits and a 256-character resource-ID limit. | HTTP integration test passed. |
| Payload-size guard | Proposed a configurable maximum serialized payload size before a record can enter the chain. | Accepted with a 65,536-byte default and a rejection test. | Focused test suite passed. |
| Response-cache control | Proposed no-store cache headers for audit API responses containing sensitive metadata. | Accepted to reduce accidental browser or intermediary caching. | HTTP integration test passed. |

## Rejected or revised AI output

- Response-only payload redaction was rejected because it made an exported redacted payload incompatible with its original `selfHash`.
- Reliance only on controller method validation for a blank account ID was rejected after the HTTP test showed the standalone route returned 200. The controller now performs the explicit boundary check.
- A purely filtered export without boundary metadata was revised because it could not demonstrate its placement in the source chain.

## Ongoing rule

For each subsequent AI-assisted stage, add a row describing the contribution, the engineer’s decision, and the validation/commit result before the stage is committed. Do not treat AI output as a substitute for human review or compliance approval.

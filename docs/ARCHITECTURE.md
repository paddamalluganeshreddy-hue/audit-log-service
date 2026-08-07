# Architecture

The service exposes a Spring MVC controller backed by an H2 `AUDIT_RECORDS` table. Each successful write is serialized by a lock, receives a server UTC timestamp, and has a SHA-256 hash calculated over canonical JSON containing its business fields, timestamp, sequence, and predecessor hash. The response includes `selfHash` and `prevHash`. Spring Boot 4 uses the Jackson 3 `tools.jackson` API explicitly.

`GET /audit/verify` recomputes each self hash and linkage in sequence order. A failure names the first sequence and either `PREVIOUS_HASH_MISMATCH` or `SELF_HASH_MISMATCH`.

## Design choices and risks

- Server timestamps avoid untrusted client clocks. A production deployment should use a trusted synchronized clock.
- SHA-256 detects alteration but does not prevent an attacker who controls the database from rewriting a whole chain. Production needs external signed checkpoints (for example KMS-backed signatures), immutable/WORM storage, database permissions, and off-site replication.
- H2 is an in-memory development database, not durable or horizontally scalable. Replace it with a managed transactionally append-only database table and restrict database roles for production.
- Sensitive top-level payload fields are replaced on ingestion with a keyed HMAC placeholder (`[REDACTED:<commitment>]`) before the record is hashed or stored. This prevents plaintext storage while keeping returned records hash-verifiable. Configure `AUDIT_REDACTION_KEY` outside source control. For full cryptographic erasure requirements, encrypt sensitive values per record and destroy the encryption key; record only the ciphertext/commitment in the hash chain.
- Retention moves expired entries into an archive collection but always verifies active plus archived records. Archive must be immutable and retained with its ordering metadata in production.
- All `/audit/*` responses send `Cache-Control: no-store` and `Pragma: no-cache` to reduce accidental storage of audit data by clients and shared intermediaries.

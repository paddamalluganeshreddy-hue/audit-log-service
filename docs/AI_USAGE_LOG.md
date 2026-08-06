# AI usage log

| Step | AI contribution | Engineer decision |
|---|---|---|
| Setup | Proposed Java 21 / Spring Boot project layout and Maven configuration. | Accepted and constrained to Spring Boot 4.x. |
| Model and chaining | Proposed immutable record fields, canonical JSON, and SHA-256 linkage. | Accepted; server timestamps were selected over client timestamps. |
| APIs | Drafted write, query, verify, archive, export, and reporting endpoints. | Modified with validation, pagination bounds, and a narrow account-access scope. |
| Retention/redaction | Suggested archive-aware verification and redacted response views. | Accepted for prototype; documented encryption/key destruction as the production solution. |
| Tests/docs | Drafted baseline chain and context tests plus implementation documentation. | Requires local execution and human review before submission. |

No AI output should be represented as independently validated without the stated review and tests.

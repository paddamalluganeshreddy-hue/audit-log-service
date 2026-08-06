# Tamper-Evident Audit Log Service

A Java 21 / Spring Boot 4 prototype for recording, querying, exporting, and verifying security audit events.

## Run

```bash
mvn spring-boot:run
```

Run validation with `mvn test`. The service listens on port 8080 by default.

## API

Write an immutable record (the server assigns timestamp and hashes):

```bash
curl -X POST http://localhost:8080/audit/write -H "Content-Type: application/json" -d '{"eventType":"ACCOUNT_ACCESS","actorId":"analyst-7","resourceType":"CLIENT_ACCOUNT","resourceId":"acct-42","payload":{"purpose":"support","token":"do-not-return"}}'
```

`GET /audit/query` supports `actorId`, `resourceType`, `resourceId`, `eventType`, `from`, `to`, `page`, and `size`. Other endpoints are `/audit/verify`, `/audit/export?actorId=...&resourceId=...`, `/audit/report?accountId=...`, and `POST /audit/retention/archive`.

See [architecture](docs/ARCHITECTURE.md), [API and assumptions](docs/API.md), and [engineering notes](docs/ENGINEERING_SUMMARY.md).

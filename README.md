# Tamper-Evident Audit Log Service

A Java 21 / Spring Boot 4 prototype for recording, querying, exporting, and verifying security audit events.

## Run

```bash
mvn spring-boot:run
```

Run validation with `mvn test`. The service listens on port 8080 by default.

## Interactive testing

Start the application, then open [Swagger UI](http://localhost:8080/swagger-ui.html) to execute every endpoint. H2 runs in-memory and creates the `AUDIT_RECORDS` table automatically; during local development inspect it at [H2 Console](http://localhost:8080/h2-console) using JDBC URL `jdbc:h2:mem:auditlog`, user `sa`, and an empty password.

## API

Write an immutable record (the server assigns timestamp and hashes):

```bash
curl -X POST http://localhost:8080/audit/write -H "Content-Type: application/json" -d '{"eventType":"ACCOUNT_ACCESS","actorId":"analyst-7","resourceType":"CLIENT_ACCOUNT","resourceId":"acct-42","payload":{"purpose":"support","token":"do-not-return"}}'
```

`GET /audit/query` supports `actorId`, `resourceType`, `resourceId`, `eventType`, `from`, `to`, `page`, and `size`. Other endpoints are `/audit/verify`, `/audit/export?actorId=...&resourceId=...`, `/audit/report?accountId=...`, and `POST /audit/retention/archive`.

See [architecture](docs/ARCHITECTURE.md), [API and assumptions](docs/API.md), and [engineering notes](docs/ENGINEERING_SUMMARY.md).

# Job Marketplace API Test Plan

## Scope and strategy

The suite verifies the public REST contract, validation and error mapping, core service rules, normalization, persistence, and ordering. Fast unit tests isolate business logic with a fixed clock and mocked repositories. MockMvc integration tests exercise HTTP routing through the real service and H2 database.

Concurrency locking and production-database compatibility are important but are not reliably proven by H2 integration tests; those belong in a PostgreSQL/Testcontainers suite.

## Test cases

| ID | Area | Scenario | Expected result | Automated in |
|---|---|---|---|---|
| JOB-01 | Create | Valid job | `201`; UUID, UTC timestamp, `OPEN`, trimmed fields | Integration + unit |
| JOB-02 | Create | Blank required field | `400`, `VALIDATION_ERROR`, field detail | Integration |
| JOB-03 | Create | Field exceeds maximum length | `400` with offending field | Integration |
| JOB-04 | Create | Unknown JSON property | `400` | Integration |
| JOB-05 | Read | Existing/non-existing job | `200` / `404 NOT_FOUND` | Integration + unit |
| JOB-06 | List | No filter and empty database | `200` and JSON array | Integration |
| JOB-07 | List | `OPEN`/`CLOSED`, case-insensitive | Only matching jobs | Integration + unit |
| JOB-08 | List | Unsupported status | `400 VALIDATION_ERROR` | Integration + unit |
| JOB-09 | List | Multiple jobs | Newest first | Integration |
| JOB-10 | Close | Open job | `200`, state becomes `CLOSED` | Integration |
| JOB-11 | Close | Already closed job | Idempotent `200`, remains `CLOSED` | Integration + unit |
| APP-01 | Apply | Valid application to open job | `201`, correct job ID | Integration |
| APP-02 | Apply | Mixed-case/spaced email and name | Values are trimmed; email lowercased | Integration + unit |
| APP-03 | Apply | Invalid email/blank name | `400` with field detail | Integration |
| APP-04 | Apply | Closed job | `409 JOB_CLOSED`; nothing saved | Integration + unit |
| APP-05 | Apply | Missing job | `404 NOT_FOUND`; nothing saved | Integration + unit |
| APP-06 | List | Existing job with no applications | `200 []` | Integration |
| APP-07 | List | Missing job | `404 NOT_FOUND` | Integration + unit |
| APP-08 | List | Multiple applications | Newest first, including after close | Integration |
| API-01 | Serialization | Success responses | Stable field names and ISO-8601 timestamps | Integration |
| API-02 | Errors | Domain and validation failures | Stable code/message/timestamp/fields envelope | Integration |

## Execution

Run `./mvnw test` on macOS/Linux or `.\mvnw.cmd test` on Windows. A passing build is the release gate. For CI, also add PostgreSQL concurrency tests that race close-versus-apply and assert an application is never committed after closure wins the row lock.

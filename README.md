# Job Marketplace API

A small REST API for managing job postings and candidate applications, built with Java 21, Spring Boot, Spring Data JPA, and H2. The project demonstrates layered backend design, request validation, transactional business rules, and integration testing.

## How to run

### Prerequisites

- Java 21
- Internet access on the first run so the Maven Wrapper can download Maven and the project dependencies

You do not need to install Maven separately.

### Start the API

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux:

```shell
chmod +x mvnw
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`. Because it uses an in-memory H2 database, all data is cleared when the application stops.

### Run the tests

```powershell
.\mvnw.cmd test
```

On macOS or Linux, use `./mvnw test`.

### Run with Docker

```shell
docker build -t job-api .
docker run --rm -p 8080:8080 job-api
```

### Available endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/jobs` | Create a new open job |
| `GET` | `/jobs/{id}` | Retrieve a job |
| `GET` | `/jobs` | List all jobs |
| `GET` | `/jobs?status=OPEN` | Filter jobs by `OPEN` or `CLOSED` status |
| `POST` | `/jobs/{id}/close` | Close a job |
| `POST` | `/jobs/{id}/applications` | Apply to an open job |
| `GET` | `/jobs/{id}/applications` | List applications for a job |

Example job request:

```json
{
  "title": "Backend Engineer",
  "description": "Build and maintain APIs",
  "location": "Kuala Lumpur / Hybrid"
}
```

Example application request:

```json
{
  "candidateName": "Ada Lovelace",
  "candidateEmail": "ada@example.com"
}
```

Successful creates return `201 Created`. Invalid input returns `400 Bad Request`, missing jobs return `404 Not Found`, and applying to a closed job returns `409 Conflict` with a stable `JOB_CLOSED` error code.

## Design Overview

The application uses a conventional layered structure:

```text
HTTP request
    ↓
Controller and request DTOs
    ↓
Application service and transaction boundary
    ↓
Spring Data repository
    ↓
H2 database
```

- **Domain layer:** `Job`, `Application`, and `JobStatus` model the core data and job state transition.
- **Web layer:** `JobController` defines the REST interface. Request and response DTOs keep the external API separate from persistence entities. Jakarta Validation rejects malformed requests before they reach the service.
- **Service layer:** `JobService` coordinates use cases and owns transaction boundaries. It contains the rule that closed jobs cannot accept applications.
- **Persistence layer:** Spring Data JPA repositories isolate data access. H2 provides realistic relational behavior without requiring an external database.
- **Error handling:** A controller advice maps domain and validation failures to consistent HTTP error responses.
- **Testing:** MockMvc integration tests exercise the complete path through routing, validation, service logic, transactions, and persistence.

Closing a job and submitting an application both acquire a pessimistic lock on the relevant job row. This ensures a concurrent application cannot be accepted while that job is being closed. Closing an already-closed job is treated as an idempotent success.

UUIDs are used for identifiers so ID generation does not depend on a database sequence. Dates are represented as UTC `Instant` values and serialized in ISO-8601 format.

## Assumptions

- Authentication and authorization are outside the requested scope; every caller can create, view, and close jobs.
- Jobs have only two statuses: `OPEN` and `CLOSED`.
- A newly created job is always `OPEN`.
- Closing a job is permanent; reopening jobs is not supported.
- Closing an already-closed job succeeds and returns the closed job.
- Candidates may apply more than once, including with the same email address, because no duplicate-application rule was specified.
- Candidate email addresses are validated and stored in lowercase, but email ownership is not verified.
- Location is intentionally free text.
- Lists are returned newest first and are expected to remain small enough that pagination is unnecessary for this exercise.
- Applications remain available for viewing after their job is closed.
- H2 data only needs to survive for the lifetime of the running process.

## What I'd improve with more time

- Replace H2 with PostgreSQL and manage schema changes with Flyway migrations.
- Add pagination and stable cursor-based ordering to job and application listings.
- Add OpenAPI/Swagger documentation and request/response examples generated from the application.
- Define a product rule for duplicate applications and enforce it with a database constraint or idempotency key.
- Add authentication and role-based authorization for employers and candidates.
- Add unit tests for domain and service behavior, repository tests, concurrency tests, and container-based PostgreSQL integration tests.
- Add production observability: structured logging, correlation IDs, metrics, tracing, and readiness/liveness endpoints.
- Add CI checks for compilation, tests, formatting, dependency scanning, and container builds.
- Add explicit production configuration for connection pooling, timeouts, graceful shutdown, and environment-based secrets.
- Consider optimistic locking or an atomic conditional database operation after measuring contention; pessimistic locking is deliberately simple and correct for the current workload.

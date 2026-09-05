# ChatGPT Handoff Context

Use this file when continuing work in a new ChatGPT conversation.

## Project summary

This repository contains a small Job Marketplace REST API built as a backend take-home exercise.

### Technology

- Java 21
- Spring Boot 3.5.5
- Spring Web
- Jakarta Validation
- Spring Data JPA
- In-memory H2 database
- JUnit 5 and MockMvc
- Maven Wrapper

### Supported behavior

- Create, retrieve, list, and filter jobs.
- Close a job.
- Submit an application to an open job.
- Reject applications to closed jobs with `409 JOB_CLOSED`.
- List applications belonging to a job.
- Validate incoming job and application data.
- Return consistent error responses.

### Architecture

- `domain`: JPA entities and job status.
- `repository`: Spring Data persistence interfaces.
- `service`: use cases, transactions, and business rules.
- `web`: REST controller, DTOs, and exception mapping.
- `src/test`: full-stack MockMvc integration tests.

Closing and applying both use a pessimistic lock on the job row to prevent a concurrent application from being accepted while a job is closing. H2 is intentionally temporary and could be replaced with PostgreSQL later.

### Verification

The Maven integration test suite passes with three tests and no failures.

## Suggested prompt for ChatGPT

> Read `README.md`, `docs/SYSTEM_DIAGRAMS.md`, and the Java source under `src/main`. This is a Java 21 Spring Boot Job Marketplace API. Explain or improve the existing Mermaid diagrams based on the actual implementation. Preserve the controller/service/repository separation and the rule that closed jobs cannot accept applications. Do not invent endpoints or behavior that are not present in the repository.

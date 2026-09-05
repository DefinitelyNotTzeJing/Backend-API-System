# System Diagrams

These diagrams use Mermaid and render directly on GitHub.

## Component and request flow

```mermaid
flowchart LR
    Client[API Client] -->|HTTP + JSON| Controller[JobController]
    Controller -->|Validated DTOs| Service[JobService]
    Service --> JobRepo[JobRepository]
    Service --> AppRepo[ApplicationRepository]
    JobRepo -->|JPA| H2[(H2 Database)]
    AppRepo -->|JPA| H2

    Validation[Jakarta Validation] -. validates .-> Controller
    Advice[ApiExceptionHandler] -. maps exceptions .-> Client

    subgraph Web Layer
        Controller
        Validation
        Advice
    end

    subgraph Application Layer
        Service
    end

    subgraph Persistence Layer
        JobRepo
        AppRepo
        H2
    end
```

## Submit application sequence

```mermaid
sequenceDiagram
    actor Candidate
    participant Controller as JobController
    participant Validator as Jakarta Validation
    participant Service as JobService
    participant Jobs as JobRepository
    participant Apps as ApplicationRepository
    participant DB as H2

    Candidate->>Controller: POST /jobs/{id}/applications
    Controller->>Validator: Validate name and email

    alt Request is invalid
        Validator-->>Candidate: 400 VALIDATION_ERROR
    else Request is valid
        Controller->>Service: apply(jobId, name, email)
        Service->>Jobs: findByIdForUpdate(jobId)
        Jobs->>DB: SELECT job with write lock
        DB-->>Service: Job

        alt Job does not exist
            Service-->>Candidate: 404 NOT_FOUND
        else Job is CLOSED
            Service-->>Candidate: 409 JOB_CLOSED
        else Job is OPEN
            Service->>Apps: save(application)
            Apps->>DB: INSERT application
            DB-->>Service: Saved application
            Service-->>Controller: Application
            Controller-->>Candidate: 201 Created
        end
    end
```

## Close job sequence

```mermaid
sequenceDiagram
    actor Employer
    participant Controller as JobController
    participant Service as JobService
    participant Jobs as JobRepository
    participant DB as H2

    Employer->>Controller: POST /jobs/{id}/close
    Controller->>Service: closeJob(id)
    Service->>Jobs: findByIdForUpdate(id)
    Jobs->>DB: SELECT job with write lock

    alt Job does not exist
        Service-->>Employer: 404 NOT_FOUND
    else Job exists
        Service->>Service: job.close()
        Service->>DB: COMMIT status = CLOSED
        Service-->>Controller: Closed job
        Controller-->>Employer: 200 OK
    end
```

The same job-row lock is used when closing and applying. Those operations therefore cannot update the same job concurrently, protecting the rule that a closed job must not accept a new application.

## Data model

```mermaid
erDiagram
    JOB ||--o{ APPLICATION : receives

    JOB {
        UUID id PK
        VARCHAR title
        VARCHAR description
        VARCHAR location
        TIMESTAMP created_at
        VARCHAR status "OPEN or CLOSED"
    }

    APPLICATION {
        UUID id PK
        UUID job_id FK
        VARCHAR candidate_name
        VARCHAR candidate_email
        TIMESTAMP submitted_at
    }
```

## Job lifecycle

```mermaid
stateDiagram-v2
    [*] --> OPEN: Job created
    OPEN --> OPEN: Accept application
    OPEN --> CLOSED: Close job
    CLOSED --> CLOSED: Close again (idempotent)
    CLOSED --> CLOSED: Application rejected with 409
```

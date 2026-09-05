package com.example.jobapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {
    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 10_000)
    private String description;

    @Column(nullable = false, length = 300)
    private String location;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    protected Job() {
    }

    public Job(UUID id, String title, String description, String location, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.createdAt = createdAt;
        this.status = JobStatus.OPEN;
    }

    public void close() {
        status = JobStatus.CLOSED;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public Instant getCreatedAt() { return createdAt; }
    public JobStatus getStatus() { return status; }
}

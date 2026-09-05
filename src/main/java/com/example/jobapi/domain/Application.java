package com.example.jobapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "applications")
public class Application {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false, length = 200)
    private String candidateName;

    @Column(nullable = false, length = 320)
    private String candidateEmail;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    protected Application() {
    }

    public Application(UUID id, Job job, String candidateName, String candidateEmail, Instant submittedAt) {
        this.id = id;
        this.job = job;
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.submittedAt = submittedAt;
    }

    public UUID getId() { return id; }
    public Job getJob() { return job; }
    public String getCandidateName() { return candidateName; }
    public String getCandidateEmail() { return candidateEmail; }
    public Instant getSubmittedAt() { return submittedAt; }
}

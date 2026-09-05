package com.example.jobapi.web.dto;

import com.example.jobapi.domain.Job;
import com.example.jobapi.domain.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(UUID id, String title, String description, String location, Instant createdAt, JobStatus status) {
    public static JobResponse from(Job job) {
        return new JobResponse(job.getId(), job.getTitle(), job.getDescription(), job.getLocation(), job.getCreatedAt(), job.getStatus());
    }
}

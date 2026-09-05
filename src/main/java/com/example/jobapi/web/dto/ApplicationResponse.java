package com.example.jobapi.web.dto;

import com.example.jobapi.domain.Application;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(UUID id, UUID jobId, String candidateName, String candidateEmail, Instant submittedAt) {
    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(application.getId(), application.getJob().getId(), application.getCandidateName(),
                application.getCandidateEmail(), application.getSubmittedAt());
    }
}

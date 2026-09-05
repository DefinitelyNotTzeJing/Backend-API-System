package com.example.jobapi.service;

import com.example.jobapi.domain.Application;
import com.example.jobapi.domain.Job;
import com.example.jobapi.domain.JobStatus;
import com.example.jobapi.repository.ApplicationRepository;
import com.example.jobapi.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-06T12:00:00Z");

    @Mock JobRepository jobs;
    @Mock ApplicationRepository applications;

    private JobService service;

    @BeforeEach
    void setUp() {
        service = new JobService(jobs, applications, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createJobTrimsFieldsAndSetsServerManagedValues() {
        when(jobs.save(org.mockito.ArgumentMatchers.any(Job.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Job result = service.createJob("  Backend Engineer  ", " Build APIs ", " Remote ");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Backend Engineer");
        assertThat(result.getDescription()).isEqualTo("Build APIs");
        assertThat(result.getLocation()).isEqualTo("Remote");
        assertThat(result.getCreatedAt()).isEqualTo(NOW);
        assertThat(result.getStatus()).isEqualTo(JobStatus.OPEN);
        verify(jobs).save(result);
    }

    @Test
    void listJobsUsesNewestFirstQueryAndParsesStatusCaseInsensitively() {
        service.listJobs(null);
        service.listJobs("closed");

        verify(jobs).findAllByOrderByCreatedAtDesc();
        verify(jobs).findByStatusOrderByCreatedAtDesc(JobStatus.CLOSED);
    }

    @Test
    void invalidStatusFailsWithoutQueryingAStatusList() {
        assertThatThrownBy(() -> service.listJobs("paused"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(jobs, never()).findByStatusOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void closeJobIsIdempotent() {
        UUID id = UUID.randomUUID();
        Job job = job(id);
        job.close();
        when(jobs.findByIdForUpdate(id)).thenReturn(Optional.of(job));

        Job result = service.closeJob(id);

        assertThat(result.getStatus()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void applyNormalizesCandidateDataAndUsesFixedClock() {
        UUID jobId = UUID.randomUUID();
        Job job = job(jobId);
        when(jobs.findByIdForUpdate(jobId)).thenReturn(Optional.of(job));
        when(applications.save(org.mockito.ArgumentMatchers.any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = service.apply(jobId, "  Ada Lovelace  ", "  ADA@Example.COM  ");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getJob()).isSameAs(job);
        assertThat(result.getCandidateName()).isEqualTo("Ada Lovelace");
        assertThat(result.getCandidateEmail()).isEqualTo("ada@example.com");
        assertThat(result.getSubmittedAt()).isEqualTo(NOW);
    }

    @Test
    void applyToClosedJobIsRejectedWithoutSaving() {
        UUID jobId = UUID.randomUUID();
        Job job = job(jobId);
        job.close();
        when(jobs.findByIdForUpdate(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.apply(jobId, "Ada", "ada@example.com"))
                .isInstanceOf(JobClosedException.class)
                .hasMessage("This job is closed and no longer accepts applications");

        verify(applications, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingJobProducesStableNotFoundErrors() {
        UUID id = UUID.randomUUID();
        when(jobs.findById(id)).thenReturn(Optional.empty());
        when(jobs.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJob(id))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Job not found");
        assertThatThrownBy(() -> service.closeJob(id))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Job not found");
        assertThatThrownBy(() -> service.apply(id, "Ada", "ada@example.com"))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Job not found");
    }

    @Test
    void listApplicationsChecksParentExistenceBeforeQuerying() {
        UUID id = UUID.randomUUID();
        when(jobs.existsById(id)).thenReturn(true);
        when(applications.findByJobIdOrderBySubmittedAtDesc(id)).thenReturn(List.of());

        assertThat(service.listApplications(id)).isEmpty();
        verify(applications).findByJobIdOrderBySubmittedAtDesc(id);
    }

    @Test
    void listApplicationsForMissingJobDoesNotQueryApplications() {
        UUID id = UUID.randomUUID();
        when(jobs.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.listApplications(id))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Job not found");
        verify(applications, never()).findByJobIdOrderBySubmittedAtDesc(id);
    }

    private Job job(UUID id) {
        return new Job(id, "Engineer", "Build APIs", "Remote", NOW.minusSeconds(60));
    }
}

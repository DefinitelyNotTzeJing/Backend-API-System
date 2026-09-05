package com.example.jobapi.service;

import com.example.jobapi.domain.Application;
import com.example.jobapi.domain.Job;
import com.example.jobapi.domain.JobStatus;
import com.example.jobapi.repository.ApplicationRepository;
import com.example.jobapi.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JobService {
    private final JobRepository jobs;
    private final ApplicationRepository applications;
    private final Clock clock;

    public JobService(JobRepository jobs, ApplicationRepository applications) {
        this(jobs, applications, Clock.systemUTC());
    }

    JobService(JobRepository jobs, ApplicationRepository applications, Clock clock) {
        this.jobs = jobs;
        this.applications = applications;
        this.clock = clock;
    }

    @Transactional
    public Job createJob(String title, String description, String location) {
        return jobs.save(new Job(UUID.randomUUID(), title.trim(), description.trim(), location.trim(), Instant.now(clock)));
    }

    public Job getJob(UUID id) {
        return jobs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    public List<Job> listJobs(String status) {
        if (status == null) return jobs.findAllByOrderByCreatedAtDesc();
        JobStatus parsed = JobStatus.valueOf(status.toUpperCase(Locale.ROOT));
        return jobs.findByStatusOrderByCreatedAtDesc(parsed);
    }

    @Transactional
    public Job closeJob(UUID id) {
        Job job = jobs.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        job.close();
        return job;
    }

    @Transactional
    public Application apply(UUID jobId, String candidateName, String candidateEmail) {
        Job job = jobs.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (job.getStatus() == JobStatus.CLOSED) throw new JobClosedException();
        return applications.save(new Application(
                UUID.randomUUID(), job, candidateName.trim(), candidateEmail.trim().toLowerCase(Locale.ROOT), Instant.now(clock)));
    }

    public List<Application> listApplications(UUID jobId) {
        if (!jobs.existsById(jobId)) throw new ResourceNotFoundException("Job not found");
        return applications.findByJobIdOrderBySubmittedAtDesc(jobId);
    }
}

package com.example.jobapi.web;

import com.example.jobapi.service.JobService;
import com.example.jobapi.web.dto.ApplicationResponse;
import com.example.jobapi.web.dto.CreateApplicationRequest;
import com.example.jobapi.web.dto.CreateJobRequest;
import com.example.jobapi.web.dto.JobResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {
    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse create(@Valid @RequestBody CreateJobRequest request) {
        return JobResponse.from(service.createJob(request.title(), request.description(), request.location()));
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable UUID id) {
        return JobResponse.from(service.getJob(id));
    }

    @GetMapping
    public List<JobResponse> list(@RequestParam(required = false) String status) {
        return service.listJobs(status).stream().map(JobResponse::from).toList();
    }

    @PostMapping("/{id}/close")
    public JobResponse close(@PathVariable UUID id) {
        return JobResponse.from(service.closeJob(id));
    }

    @PostMapping("/{id}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@PathVariable UUID id, @Valid @RequestBody CreateApplicationRequest request) {
        return ApplicationResponse.from(service.apply(id, request.candidateName(), request.candidateEmail()));
    }

    @GetMapping("/{id}/applications")
    public List<ApplicationResponse> applications(@PathVariable UUID id) {
        return service.listApplications(id).stream().map(ApplicationResponse::from).toList();
    }
}

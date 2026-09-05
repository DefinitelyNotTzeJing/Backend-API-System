package com.example.jobapi.repository;

import com.example.jobapi.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    List<Application> findByJobIdOrderBySubmittedAtDesc(UUID jobId);
}

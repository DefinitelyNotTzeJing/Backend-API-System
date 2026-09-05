package com.example.jobapi.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotBlank @Size(max = 200) String candidateName,
        @NotBlank @Email @Size(max = 320) String candidateEmail) {
}

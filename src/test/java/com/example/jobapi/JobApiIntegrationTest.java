package com.example.jobapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.jobapi.repository.ApplicationRepository;
import com.example.jobapi.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobApiApplication.class)
@AutoConfigureMockMvc
class JobApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ApplicationRepository applications;
    @Autowired JobRepository jobs;

    @BeforeEach
    void clearDatabase() {
        applications.deleteAll();
        jobs.deleteAll();
    }

    @Test
    void supportsTheJobLifecycleAndRejectsApplicationsAfterClosing() throws Exception {
        String created = mvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backend Engineer","description":"Build APIs","location":"Remote"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        String jobId = objectMapper.readTree(created).get("id").asText();

        mvc.perform(post("/jobs/{id}/applications", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"candidateName":"Ada Lovelace","candidateEmail":"ADA@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.candidateEmail").value("ada@example.com"));

        mvc.perform(get("/jobs/{id}/applications", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(post("/jobs/{id}/close", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mvc.perform(post("/jobs/{id}/applications", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"candidateName":"Grace Hopper","candidateEmail":"grace@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_CLOSED"));
    }

    @Test
    void filtersJobsByStatus() throws Exception {
        String created = createJob("Open role");
        String jobId = objectMapper.readTree(created).get("id").asText();
        mvc.perform(post("/jobs/{id}/close", jobId)).andExpect(status().isOk());
        createJob("Another open role");

        mvc.perform(get("/jobs").param("status", "closed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(jobId));
    }

    @Test
    void createsAJobWithTrimmedFieldsAndACompleteResponseContract() throws Exception {
        mvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  Backend Engineer  ","description":"  Build APIs  ","location":"  Remote  "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", matchesPattern("[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.title").value("Backend Engineer"))
                .andExpect(jsonPath("$.description").value("Build APIs"))
                .andExpect(jsonPath("$.location").value("Remote"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void closingAJobTwiceIsAnIdempotentSuccess() throws Exception {
        String jobId = objectMapper.readTree(createJob("Close me")).get("id").asText();

        mvc.perform(post("/jobs/{id}/close", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        mvc.perform(post("/jobs/{id}/close", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void returnsEmptyCollectionsAndNotFoundForApplicationsOfMissingJob() throws Exception {
        String jobId = objectMapper.readTree(createJob("No applicants yet")).get("id").asText();

        mvc.perform(get("/jobs/{id}/applications", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/jobs/{id}/applications", "21fa90da-1648-4bc8-9657-f9432bb2c623"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Job not found"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.fields").isEmpty());
    }

    @Test
    void validatesApplicationFieldsAndDoesNotPersistRejectedInput() throws Exception {
        String jobId = objectMapper.readTree(createJob("Validated role")).get("id").asText();

        mvc.perform(post("/jobs/{id}/applications", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"candidateName":" ","candidateEmail":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fields.candidateName").exists())
                .andExpect(jsonPath("$.fields.candidateEmail").exists());

        mvc.perform(get("/jobs/{id}/applications", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsOversizedAndUnknownJobFields() throws Exception {
        JsonNode oversized = objectMapper.createObjectNode()
                .put("title", "x".repeat(201))
                .put("description", "Description")
                .put("location", "Remote");
        mvc.perform(post("/jobs").contentType(MediaType.APPLICATION_JSON).content(oversized.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists());

        mvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Engineer","description":"Build","location":"Remote","status":"CLOSED"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void applyingToMissingJobDoesNotCreateAnApplication() throws Exception {
        String missingId = "21fa90da-1648-4bc8-9657-f9432bb2c623";

        mvc.perform(post("/jobs/{id}/applications", missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"candidateName":"Ada Lovelace","candidateEmail":"ada@example.com"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        org.assertj.core.api.Assertions.assertThat(applications.count()).isZero();
    }

    @Test
    void validatesRequestsAndMissingResources() throws Exception {
        mvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","description":"Description","location":"Remote"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.title").exists());

        mvc.perform(get("/jobs/{id}", "21fa90da-1648-4bc8-9657-f9432bb2c623"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mvc.perform(get("/jobs").param("status", "paused"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private String createJob(String title) throws Exception {
        JsonNode body = objectMapper.createObjectNode()
                .put("title", title)
                .put("description", "Description")
                .put("location", "Remote");
        return mvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}

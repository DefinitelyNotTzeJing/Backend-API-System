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

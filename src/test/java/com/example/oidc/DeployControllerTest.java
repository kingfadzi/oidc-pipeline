package com.example.oidc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DeployControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testDeployEndpoint_validToken_success() throws Exception {
        Jwt jwt = createValidJwt();

        mockMvc.perform(post("/api/v1/deploy")
                .with(jwt().jwt(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": \"deploy\", \"version\": \"v1.2.3\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Deployment accepted"))
            .andExpect(jsonPath("$.validated.namespace_path").value("myorg/platform"))
            .andExpect(jsonPath("$.validated.project_path").value("myorg/platform/core-api"))
            .andExpect(jsonPath("$.validated.branch").value("main"))
            .andExpect(jsonPath("$.validated.pipeline_source").value("push"))
            .andExpect(jsonPath("$.validated.environment").value("production"))
            .andExpect(jsonPath("$.validated.ref_protected").value(true))
            .andExpect(jsonPath("$.audit.user").exists())
            .andExpect(jsonPath("$.audit.pipeline_id").value("12345"))
            .andExpect(jsonPath("$.audit.job_id").value("67890"))
            .andExpect(jsonPath("$.audit.commit_sha").value("abc123def456"))
            .andExpect(jsonPath("$.request.action").value("deploy"))
            .andExpect(jsonPath("$.request.version").value("v1.2.3"));
    }

    @Test
    void testDeployEndpoint_missingToken_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/deploy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": \"deploy\", \"version\": \"v1.2.3\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testHealthEndpoint_publicAccess() throws Exception {
        mockMvc.perform(post("/actuator/health"))
            .andExpect(status().isMethodNotAllowed()); // Health is GET, not POST

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/actuator/health"))
            .andExpect(status().isOk());
    }

    private Jwt createValidJwt() {
        return Jwt.withTokenValue("valid-token")
            .header("alg", "RS256")
            .claim("aud", List.of("https://api.butterflycluster.com"))
            .claim("namespace_path", "myorg/platform")
            .claim("project_path", "myorg/platform/core-api")
            .claim("ref", "refs/heads/main")
            .claim("pipeline_source", "push")
            .claim("environment", "production")
            .claim("ref_protected", true)
            .claim("user_login", "testuser")
            .claim("sub", "project_path:myorg/platform/core-api:ref_type:branch:ref:main:user:testuser")
            .claim("pipeline_id", "12345")
            .claim("job_id", "67890")
            .claim("sha", "abc123def456")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }
}

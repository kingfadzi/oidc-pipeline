package com.example.oidc;

import com.example.oidc.model.WorkspaceConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class DeployController {

    @Autowired
    private WorkspaceConfig workspaceConfig;

    @PostMapping("/deploy")
    public ResponseEntity<?> deploy(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody DeployRequest request,
            HttpServletRequest httpRequest) {

        // Extract validated fields (blocking validators)
        String namespacePath = jwt.getClaimAsString("namespace_path");
        String projectPath = jwt.getClaimAsString("project_path");
        String ref = jwt.getClaimAsString("ref");
        String branch = ref != null ? ref.replace("refs/heads/", "") : ref;
        String pipelineSource = jwt.getClaimAsString("pipeline_source");
        String environment = jwt.getClaimAsString("environment");
        Boolean refProtected = jwt.getClaim("ref_protected");
        String audience = jwt.getAudience() != null && !jwt.getAudience().isEmpty()
            ? jwt.getAudience().get(0) : null;

        // Look up product from workspace config
        String product = workspaceConfig.getWorkspaces().stream()
            .filter(ws -> ws.getNamespace().equals(namespacePath))
            .map(WorkspaceConfig.Workspace::getProduct)
            .findFirst()
            .orElse(null);

        // Extract audit fields (logging only)
        String user = jwt.getClaimAsString("user_login");
        String sub = jwt.getClaimAsString("sub");
        String pipelineId = jwt.getClaimAsString("pipeline_id");
        String jobId = jwt.getClaimAsString("job_id");
        String sha = jwt.getClaimAsString("sha");
        Instant issuedAt = jwt.getIssuedAt();
        Instant expiresAt = jwt.getExpiresAt();
        String ipAddress = httpRequest.getRemoteAddr();

        // Log audit trail
        log.info("Deployment request accepted - Namespace: {}, Project: {}, Branch: {}, User: {}, Pipeline: {}, Job: {}, SHA: {}, IP: {}, Environment: {}",
            namespacePath, projectPath, branch, user != null ? user : sub, pipelineId, jobId, sha, ipAddress, environment);

        // Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("message", "Deployment accepted");

        // Validated fields
        Map<String, Object> validated = new LinkedHashMap<>();
        validated.put("audience", audience);
        validated.put("namespace_path", namespacePath);
        validated.put("project_path", projectPath);
        validated.put("product", product);
        validated.put("branch", branch);
        validated.put("pipeline_source", pipelineSource);
        validated.put("environment", environment);
        validated.put("ref_protected", refProtected);
        response.put("validated", validated);

        // Audit fields
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("user", user != null ? user : sub);
        audit.put("pipeline_id", pipelineId);
        audit.put("job_id", jobId);
        audit.put("commit_sha", sha);
        audit.put("issued_at", issuedAt);
        audit.put("expires_at", expiresAt);
        audit.put("ip_address", ipAddress);
        response.put("audit", audit);

        // Request details
        Map<String, Object> requestDetails = new LinkedHashMap<>();
        requestDetails.put("action", request.getAction());
        requestDetails.put("version", request.getVersion());
        response.put("request", requestDetails);

        return ResponseEntity.ok(response);
    }

    @Data
    static class DeployRequest {
        private String action;
        private String version;
    }
}

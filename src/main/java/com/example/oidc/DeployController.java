package com.example.oidc;

import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DeployController {

    @PostMapping("/deploy")
    public ResponseEntity<?> deploy(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody DeployRequest request) {

        String namespace = jwt.getClaimAsString("namespace_path");
        String project = jwt.getClaimAsString("project_path");
        String branch = jwt.getClaimAsString("ref");

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Deployment accepted",
            "namespace", namespace,
            "project", project,
            "branch", branch,
            "version", request.getVersion()
        ));
    }

    @Data
    static class DeployRequest {
        private String action;
        private String version;
    }
}

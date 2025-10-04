package com.example.oidc.config;

import com.example.oidc.model.WorkspaceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${gitlab.oidc.issuer}")
    private String issuer;

    @Value("${gitlab.oidc.audience}")
    private String audience;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, WorkspaceConfig workspaceConfig) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder(workspaceConfig)))
            );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(WorkspaceConfig workspaceConfig) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withIssuerLocation(issuer)
            .build();

        OAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            audienceValidator(),
            branchValidator(workspaceConfig),
            workspaceValidator(workspaceConfig),
            projectPathValidator(workspaceConfig),
            pipelineSourceValidator(),
            environmentValidator(workspaceConfig),
            protectedBranchValidator()
        );

        decoder.setJwtValidator(validators);
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> audienceValidator() {
        return token -> {
            List<String> audiences = token.getAudience();
            if (audiences.contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                new org.springframework.security.oauth2.core.OAuth2Error(
                    "invalid_token", "Invalid audience", null)
            );
        };
    }

    private OAuth2TokenValidator<Jwt> branchValidator(WorkspaceConfig config) {
        return token -> {
            String ref = token.getClaimAsString("ref");
            String branch = ref != null ? ref.replace("refs/heads/", "") : null;
            String namespacePath = token.getClaimAsString("namespace_path");

            if (branch == null) {
                return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                        "invalid_token", "Missing branch reference", null)
                );
            }

            return config.getWorkspaces().stream()
                .filter(ws -> ws.getNamespace().equals(namespacePath))
                .findFirst()
                .map(ws -> {
                    if (ws.getBranches() == null || ws.getBranches().isEmpty()) {
                        return OAuth2TokenValidatorResult.success();
                    }
                    if (ws.getBranches().contains(branch)) {
                        return OAuth2TokenValidatorResult.success();
                    }
                    return OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error(
                            "invalid_token", "Branch '" + branch + "' not allowed", null)
                    );
                })
                .orElse(OAuth2TokenValidatorResult.success());
        };
    }

    private OAuth2TokenValidator<Jwt> workspaceValidator(WorkspaceConfig config) {
        return token -> {
            String namespacePath = token.getClaimAsString("namespace_path");

            if (namespacePath == null) {
                return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                        "invalid_token", "Missing namespace_path", null)
                );
            }

            boolean allowed = config.getWorkspaces().stream()
                .anyMatch(ws -> ws.getNamespace().equals(namespacePath));

            if (allowed) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(
                new org.springframework.security.oauth2.core.OAuth2Error(
                    "invalid_token", "Workspace not allowed", null)
            );
        };
    }

    private OAuth2TokenValidator<Jwt> projectPathValidator(WorkspaceConfig config) {
        return token -> {
            String projectPath = token.getClaimAsString("project_path");
            String namespacePath = token.getClaimAsString("namespace_path");

            if (projectPath == null) {
                return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                        "invalid_token", "Missing project_path", null)
                );
            }

            return config.getWorkspaces().stream()
                .filter(ws -> ws.getNamespace().equals(namespacePath))
                .findFirst()
                .map(ws -> {
                    if (ws.getProject() == null || ws.getProject().isEmpty()) {
                        return OAuth2TokenValidatorResult.success();
                    }
                    if (ws.getProject().equals(projectPath)) {
                        return OAuth2TokenValidatorResult.success();
                    }
                    return OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error(
                            "invalid_token", "Project '" + projectPath + "' not allowed", null)
                    );
                })
                .orElse(OAuth2TokenValidatorResult.success());
        };
    }

    private OAuth2TokenValidator<Jwt> pipelineSourceValidator() {
        return token -> {
            String pipelineSource = token.getClaimAsString("pipeline_source");

            if (pipelineSource == null) {
                return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                        "invalid_token", "Missing pipeline_source", null)
                );
            }

            List<String> allowedSources = List.of("push", "web");
            if (allowedSources.contains(pipelineSource)) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(
                new org.springframework.security.oauth2.core.OAuth2Error(
                    "invalid_token", "Pipeline source '" + pipelineSource + "' not allowed", null)
            );
        };
    }

    private OAuth2TokenValidator<Jwt> environmentValidator(WorkspaceConfig config) {
        return token -> {
            String environment = token.getClaimAsString("environment");
            String namespacePath = token.getClaimAsString("namespace_path");

            // Environment is optional in GitLab CI, so allow if not present
            if (environment == null) {
                return OAuth2TokenValidatorResult.success();
            }

            return config.getWorkspaces().stream()
                .filter(ws -> ws.getNamespace().equals(namespacePath))
                .findFirst()
                .map(ws -> {
                    if (ws.getEnvironments() == null || ws.getEnvironments().isEmpty()) {
                        return OAuth2TokenValidatorResult.success();
                    }
                    if (ws.getEnvironments().contains(environment)) {
                        return OAuth2TokenValidatorResult.success();
                    }
                    return OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error(
                            "invalid_token", "Environment '" + environment + "' not allowed", null)
                    );
                })
                .orElse(OAuth2TokenValidatorResult.success());
        };
    }

    private OAuth2TokenValidator<Jwt> protectedBranchValidator() {
        return token -> {
            Boolean refProtected = token.getClaim("ref_protected");

            if (refProtected == null) {
                return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                        "invalid_token", "Missing ref_protected claim", null)
                );
            }

            if (Boolean.TRUE.equals(refProtected)) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(
                new org.springframework.security.oauth2.core.OAuth2Error(
                    "invalid_token", "Branch must be protected", null)
            );
        };
    }
}

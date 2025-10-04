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
            branchValidator(),
            workspaceValidator(workspaceConfig)
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

    private OAuth2TokenValidator<Jwt> branchValidator() {
        return token -> {
            String branch = token.getClaimAsString("ref");
            if ("refs/heads/main".equals(branch) || "main".equals(branch)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                new org.springframework.security.oauth2.core.OAuth2Error(
                    "invalid_token", "Branch must be main", null)
            );
        };
    }

    private OAuth2TokenValidator<Jwt> workspaceValidator(WorkspaceConfig config) {
        return token -> {
            String namespacePath = token.getClaimAsString("namespace_path");
            String projectPath = token.getClaimAsString("project_path");

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
}

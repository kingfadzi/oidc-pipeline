package com.example.oidc.config;

import com.example.oidc.model.WorkspaceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    private WorkspaceConfig workspaceConfig;

    @BeforeEach
    void setUp() {
        workspaceConfig = new WorkspaceConfig();
        WorkspaceConfig.Workspace workspace1 = new WorkspaceConfig.Workspace();
        workspace1.setNamespace("myorg/platform");
        workspace1.setProject("myorg/platform/core-api");
        workspace1.setProduct("core-api");
        workspace1.setBranches(List.of("main", "production"));
        workspace1.setEnvironments(List.of("production", "staging"));

        WorkspaceConfig.Workspace workspace2 = new WorkspaceConfig.Workspace();
        workspace2.setNamespace("myorg/services");
        workspace2.setProject("myorg/services/payment-service");
        workspace2.setProduct("payment-service");
        workspace2.setBranches(List.of("main"));
        workspace2.setEnvironments(List.of("production"));

        workspaceConfig.setWorkspaces(List.of(workspace1, workspace2));
    }

    @Test
    void testAudienceValidator_validAudience() {
        Jwt jwt = createJwt(Map.of("aud", List.of("https://api.butterflycluster.com")));

        SecurityConfig config = new SecurityConfig();
        var result = config.audienceValidator().validate(jwt);

        assertTrue(result.hasErrors() == false);
    }

    @Test
    void testBranchValidator_allowedBranch() {
        Jwt jwt = createJwt(Map.of(
            "ref", "refs/heads/main",
            "namespace_path", "myorg/platform"
        ));

        SecurityConfig config = new SecurityConfig();
        var result = config.branchValidator(workspaceConfig).validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    void testBranchValidator_disallowedBranch() {
        Jwt jwt = createJwt(Map.of(
            "ref", "refs/heads/feature-branch",
            "namespace_path", "myorg/platform"
        ));

        SecurityConfig config = new SecurityConfig();
        var result = config.branchValidator(workspaceConfig).validate(jwt);

        assertTrue(result.hasErrors());
    }

    @Test
    void testProjectPathValidator_validProject() {
        Jwt jwt = createJwt(Map.of(
            "project_path", "myorg/platform/core-api",
            "namespace_path", "myorg/platform"
        ));

        SecurityConfig config = new SecurityConfig();
        var result = config.projectPathValidator(workspaceConfig).validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    void testProjectPathValidator_invalidProject() {
        Jwt jwt = createJwt(Map.of(
            "project_path", "myorg/platform/wrong-project",
            "namespace_path", "myorg/platform"
        ));

        SecurityConfig config = new SecurityConfig();
        var result = config.projectPathValidator(workspaceConfig).validate(jwt);

        assertTrue(result.hasErrors());
    }

    @Test
    void testPipelineSourceValidator_allowedSource() {
        Jwt jwt = createJwt(Map.of("pipeline_source", "push"));

        SecurityConfig config = new SecurityConfig();
        var result = config.pipelineSourceValidator().validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    void testPipelineSourceValidator_blockedSource() {
        Jwt jwt = createJwt(Map.of("pipeline_source", "api"));

        SecurityConfig config = new SecurityConfig();
        var result = config.pipelineSourceValidator().validate(jwt);

        assertTrue(result.hasErrors());
    }

    @Test
    void testEnvironmentValidator_allowedEnvironment() {
        Jwt jwt = createJwt(Map.of(
            "environment", "production",
            "namespace_path", "myorg/platform"
        ));

        SecurityConfig config = new SecurityConfig();
        var result = config.environmentValidator(workspaceConfig).validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    void testEnvironmentValidator_disallowedEnvironment() {
        Jwt jwt = createJwt(Map.of(
            "environment", "development",
            "namespace_path", "myorg/platform"
        ));

        SecurityConfig config = new SecurityConfig();
        var result = config.environmentValidator(workspaceConfig).validate(jwt);

        assertTrue(result.hasErrors());
    }

    @Test
    void testProtectedBranchValidator_protected() {
        Jwt jwt = createJwt(Map.of("ref_protected", true));

        SecurityConfig config = new SecurityConfig();
        var result = config.protectedBranchValidator().validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    void testProtectedBranchValidator_notProtected() {
        Jwt jwt = createJwt(Map.of("ref_protected", false));

        SecurityConfig config = new SecurityConfig();
        var result = config.protectedBranchValidator().validate(jwt);

        assertTrue(result.hasErrors());
    }

    @Test
    void testWorkspaceValidator_allowedNamespace() {
        Jwt jwt = createJwt(Map.of("namespace_path", "myorg/platform"));

        SecurityConfig config = new SecurityConfig();
        var result = config.workspaceValidator(workspaceConfig).validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    void testWorkspaceValidator_disallowedNamespace() {
        Jwt jwt = createJwt(Map.of("namespace_path", "unauthorized/namespace"));

        SecurityConfig config = new SecurityConfig();
        var result = config.workspaceValidator(workspaceConfig).validate(jwt);

        assertTrue(result.hasErrors());
    }

    private Jwt createJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claims(c -> c.putAll(claims))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }
}

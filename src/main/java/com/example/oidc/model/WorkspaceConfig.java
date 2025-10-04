package com.example.oidc.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "allowed")
public class WorkspaceConfig {
    private List<Workspace> workspaces;

    @Data
    public static class Workspace {
        private String namespace;
        private String project;
        private String product;
        private List<String> branches;
        private List<String> environments;
    }
}

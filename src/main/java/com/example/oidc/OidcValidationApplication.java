package com.example.oidc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OidcValidationApplication {
    public static void main(String[] args) {
        SpringApplication.run(OidcValidationApplication.class, args);
    }
}

package ch.babyguess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("babyguess.admin")
public record AdminProperties(String username, String password) {

    public AdminProperties {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("The administrator username must be configured");
        }
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("The administrator password must contain at least 12 characters");
        }
    }
}

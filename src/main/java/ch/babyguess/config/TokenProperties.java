package ch.babyguess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("babyguess.tokens")
public record TokenProperties(String secret) {

    public TokenProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("The participant token secret must contain at least 32 characters");
        }
    }
}

package ch.babyguess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("babyguess.secrets")
public record SecretEncryptionProperties(String encryptionKey) {
}

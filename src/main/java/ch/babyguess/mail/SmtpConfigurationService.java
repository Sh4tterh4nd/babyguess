package ch.babyguess.mail;

import java.time.Clock;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmtpConfigurationService {

    private final SmtpConfigurationRepository repository;
    private final SmtpSecretCipher secretCipher;
    private final Environment environment;
    private final Clock clock;

    public SmtpConfigurationService(
            SmtpConfigurationRepository repository,
            SmtpSecretCipher secretCipher,
            Environment environment,
            Clock clock) {
        this.repository = repository;
        this.secretCipher = secretCipher;
        this.environment = environment;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SmtpConfigurationStatus status() {
        var stored = storedConfiguration();
        var source = activeSource(stored);
        var ready = switch (source) {
            case DATABASE -> databaseReady(stored);
            case ENVIRONMENT -> true;
            case NONE -> false;
        };
        return new SmtpConfigurationStatus(
                stored.getVersion(),
                stored.isEnabled(),
                stored.getHost(),
                stored.getPort(),
                stored.getTransportSecurity(),
                stored.isAuthenticationRequired(),
                stored.getSenderAddress(),
                stored.getUsername(),
                hasText(stored.getEncryptedPassword()),
                secretCipher.isAvailable(),
                source,
                ready);
    }

    @Transactional
    public void update(
            long expectedVersion,
            boolean enabled,
            String host,
            int port,
            SmtpTransportSecurity transportSecurity,
            boolean authenticationRequired,
            String senderAddress,
            String username,
            String replacementPassword) {
        var stored = storedConfiguration();
        if (stored.getVersion() != expectedVersion) {
            throw rejected(SmtpConfigurationProblem.STALE);
        }
        if (enabled && !hasText(host)) {
            throw rejected(SmtpConfigurationProblem.HOST_REQUIRED);
        }
        if (enabled && !hasText(senderAddress)) {
            throw rejected(SmtpConfigurationProblem.SENDER_REQUIRED);
        }
        if (enabled && authenticationRequired && !hasText(username)) {
            throw rejected(SmtpConfigurationProblem.USERNAME_REQUIRED);
        }

        var encryptedPassword = stored.getEncryptedPassword();
        if (!authenticationRequired) {
            encryptedPassword = null;
        } else if (hasText(replacementPassword)) {
            if (!secretCipher.isAvailable()) {
                throw rejected(SmtpConfigurationProblem.ENCRYPTION_KEY_REQUIRED);
            }
            encryptedPassword = secretCipher.encrypt(replacementPassword);
        }
        if (enabled && authenticationRequired && !hasText(encryptedPassword)) {
            throw rejected(SmtpConfigurationProblem.PASSWORD_REQUIRED);
        }

        stored.update(
                enabled,
                host,
                port,
                transportSecurity,
                authenticationRequired,
                senderAddress,
                username,
                encryptedPassword,
                clock.instant());
    }

    @Transactional(readOnly = true)
    SmtpConnectionSettings activeConfiguration() {
        var stored = storedConfiguration();
        if (stored.isEnabled()) {
            if (!databaseStructurallyComplete(stored)) {
                throw new MailUnavailableException();
            }
            var password = stored.isAuthenticationRequired()
                    ? secretCipher.decrypt(stored.getEncryptedPassword())
                    : null;
            return new SmtpConnectionSettings(
                    stored.getHost(),
                    stored.getPort(),
                    stored.getTransportSecurity(),
                    stored.isAuthenticationRequired(),
                    stored.getSenderAddress(),
                    stored.getUsername(),
                    password);
        }
        return environmentConfiguration();
    }

    private SmtpConnectionSettings environmentConfiguration() {
        var host = environment.getProperty("spring.mail.host");
        if (!hasText(host)) {
            throw new MailUnavailableException();
        }
        var username = environment.getProperty("spring.mail.username");
        var authenticationRequired = environment.getProperty(
                "spring.mail.properties.mail.smtp.auth", Boolean.class, hasText(username));
        var security = environment.getProperty("spring.mail.ssl.enabled", Boolean.class, false)
                ? SmtpTransportSecurity.SSL_TLS
                : environment.getProperty(
                        "spring.mail.properties.mail.smtp.starttls.enable", Boolean.class, false)
                        ? SmtpTransportSecurity.STARTTLS
                        : SmtpTransportSecurity.NONE;
        return new SmtpConnectionSettings(
                host.strip(),
                environment.getProperty("spring.mail.port", Integer.class, 587),
                security,
                authenticationRequired,
                stripToNull(environment.getProperty("babyguess.mail.from")),
                stripToNull(username),
                environment.getProperty("spring.mail.password"));
    }

    private SmtpConfiguration storedConfiguration() {
        return repository.findById(SmtpConfiguration.SINGLETON_ID).orElseThrow();
    }

    private OutgoingMailConfigurationSource activeSource(SmtpConfiguration stored) {
        if (stored.isEnabled()) {
            return OutgoingMailConfigurationSource.DATABASE;
        }
        return hasText(environment.getProperty("spring.mail.host"))
                ? OutgoingMailConfigurationSource.ENVIRONMENT
                : OutgoingMailConfigurationSource.NONE;
    }

    private boolean databaseReady(SmtpConfiguration stored) {
        return databaseStructurallyComplete(stored)
                && (!stored.isAuthenticationRequired() || secretCipher.isAvailable());
    }

    private boolean databaseStructurallyComplete(SmtpConfiguration stored) {
        return hasText(stored.getHost())
                && stored.getPort() >= 1
                && stored.getPort() <= 65535
                && stored.getTransportSecurity() != null
                && hasText(stored.getSenderAddress())
                && (!stored.isAuthenticationRequired()
                        || hasText(stored.getUsername())
                        && hasText(stored.getEncryptedPassword()));
    }

    private SmtpConfigurationRejectedException rejected(SmtpConfigurationProblem problem) {
        return new SmtpConfigurationRejectedException(problem);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String stripToNull(String value) {
        return hasText(value) ? value.strip() : null;
    }
}

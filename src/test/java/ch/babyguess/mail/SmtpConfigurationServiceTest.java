package ch.babyguess.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.babyguess.config.SecretEncryptionProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SmtpConfigurationServiceTest {

    @Test
    void refusesToPersistANewPasswordWithoutAnEncryptionKey() {
        var repository = mock(SmtpConfigurationRepository.class);
        var stored = mock(SmtpConfiguration.class);
        when(repository.findById(SmtpConfiguration.SINGLETON_ID)).thenReturn(Optional.of(stored));
        when(stored.getVersion()).thenReturn(3L);
        var service = new SmtpConfigurationService(
                repository,
                new SmtpSecretCipher(new SecretEncryptionProperties("")),
                new MockEnvironment(),
                Clock.fixed(Instant.parse("2026-09-06T00:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(() -> service.update(
                3,
                true,
                "smtp.example.test",
                587,
                SmtpTransportSecurity.STARTTLS,
                true,
                "hello@example.test",
                "smtp-user",
                "must-not-be-stored"))
                .isInstanceOf(SmtpConfigurationRejectedException.class)
                .extracting("problem")
                .isEqualTo(SmtpConfigurationProblem.ENCRYPTION_KEY_REQUIRED);

        verify(stored, never()).update(
                anyBoolean(), any(), anyInt(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void usesSpringMailEnvironmentSettingsWhenSavedSettingsAreDisabled() {
        var repository = mock(SmtpConfigurationRepository.class);
        var stored = mock(SmtpConfiguration.class);
        when(repository.findById(SmtpConfiguration.SINGLETON_ID)).thenReturn(Optional.of(stored));
        when(stored.isEnabled()).thenReturn(false);
        var environment = new MockEnvironment()
                .withProperty("spring.mail.host", "smtp.environment.test")
                .withProperty("spring.mail.port", "465")
                .withProperty("spring.mail.username", "environment-user")
                .withProperty("spring.mail.password", "environment-secret")
                .withProperty("spring.mail.ssl.enabled", "true")
                .withProperty("babyguess.mail.from", "hello@example.test");
        var service = new SmtpConfigurationService(
                repository,
                new SmtpSecretCipher(new SecretEncryptionProperties("")),
                environment,
                Clock.systemUTC());

        var settings = service.activeConfiguration();

        assertThat(settings.host()).isEqualTo("smtp.environment.test");
        assertThat(settings.port()).isEqualTo(465);
        assertThat(settings.transportSecurity()).isEqualTo(SmtpTransportSecurity.SSL_TLS);
        assertThat(settings.authenticationRequired()).isTrue();
        assertThat(settings.senderAddress()).isEqualTo("hello@example.test");
    }
}

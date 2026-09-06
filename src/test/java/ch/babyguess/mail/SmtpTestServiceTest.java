package ch.babyguess.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailAuthenticationException;

class SmtpTestServiceTest {

    @Test
    void reducesAuthenticationErrorsToASafeResultCode() {
        var outgoing = mock(OutgoingMailService.class);
        var messages = mock(MessageSource.class);
        when(messages.getMessage("mail.test.subject", null, Locale.ENGLISH)).thenReturn("Test");
        when(messages.getMessage("mail.test.body", null, Locale.ENGLISH)).thenReturn("Body");
        doThrow(new MailAuthenticationException("provider response contained sensitive details"))
                .when(outgoing).send(org.mockito.ArgumentMatchers.any());

        var result = new SmtpTestService(outgoing, messages)
                .send("host@example.test", Locale.ENGLISH);

        assertThat(result).isEqualTo(SmtpTestResult.AUTHENTICATION_FAILED);
    }

    @Test
    void distinguishesAnUnavailableEncryptionKeyWithoutReturningItsException() {
        var outgoing = mock(OutgoingMailService.class);
        var messages = mock(MessageSource.class);
        when(messages.getMessage("mail.test.subject", null, Locale.ENGLISH)).thenReturn("Test");
        when(messages.getMessage("mail.test.body", null, Locale.ENGLISH)).thenReturn("Body");
        doThrow(new MailUnavailableException(MailUnavailableException.Reason.ENCRYPTION_KEY_UNAVAILABLE))
                .when(outgoing).send(org.mockito.ArgumentMatchers.any());

        var result = new SmtpTestService(outgoing, messages)
                .send("host@example.test", Locale.ENGLISH);

        assertThat(result).isEqualTo(SmtpTestResult.CREDENTIAL_UNAVAILABLE);
    }
}

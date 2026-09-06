package ch.babyguess.mail;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class SmtpTestService {

    private final OutgoingMailService outgoingMailService;
    private final MessageSource messageSource;

    public SmtpTestService(OutgoingMailService outgoingMailService, MessageSource messageSource) {
        this.outgoingMailService = outgoingMailService;
        this.messageSource = messageSource;
    }

    public SmtpTestResult send(String recipient, Locale locale) {
        var message = new SimpleMailMessage();
        message.setTo(recipient.strip());
        message.setSubject(messageSource.getMessage("mail.test.subject", null, locale));
        message.setText(messageSource.getMessage("mail.test.body", null, locale));
        try {
            outgoingMailService.send(message);
            return SmtpTestResult.SENT;
        } catch (MailUnavailableException exception) {
            return exception.getReason() == MailUnavailableException.Reason.NOT_CONFIGURED
                    ? SmtpTestResult.NOT_CONFIGURED
                    : SmtpTestResult.CREDENTIAL_UNAVAILABLE;
        } catch (MailAuthenticationException exception) {
            return SmtpTestResult.AUTHENTICATION_FAILED;
        } catch (RuntimeException exception) {
            return SmtpTestResult.DELIVERY_FAILED;
        }
    }
}

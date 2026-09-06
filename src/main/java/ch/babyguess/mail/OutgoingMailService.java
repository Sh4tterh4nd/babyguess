package ch.babyguess.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class OutgoingMailService {

    private final SmtpConfigurationService configurationService;
    private final MailSenderFactory mailSenderFactory;

    OutgoingMailService(
            SmtpConfigurationService configurationService,
            MailSenderFactory mailSenderFactory) {
        this.configurationService = configurationService;
        this.mailSenderFactory = mailSenderFactory;
    }

    public void send(SimpleMailMessage original) {
        var settings = configurationService.activeConfiguration();
        var message = new SimpleMailMessage(original);
        if (settings.senderAddress() != null) {
            message.setFrom(settings.senderAddress());
        }
        mailSenderFactory.create(settings).send(message);
    }
}

package ch.babyguess.mail;

import ch.babyguess.participant.Participant;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;

class SmtpRevealResultNotifier implements RevealResultNotifier {

    private final OutgoingMailService outgoingMailService;
    private final MessageSource messageSource;

    SmtpRevealResultNotifier(OutgoingMailService outgoingMailService, MessageSource messageSource) {
        this.outgoingMailService = outgoingMailService;
        this.messageSource = messageSource;
    }

    @Override
    public void send(Participant participant, Locale locale, RevealEmailContent content) {
        var message = new SimpleMailMessage();
        message.setTo(participant.getEmailAddress());
        message.setSubject(messageSource.getMessage(
                "mail.reveal.subject", new Object[] {content.actualName()}, locale));
        message.setText(messageSource.getMessage(
                "mail.reveal.body",
                new Object[] {
                    participant.getDisplayName(),
                    content.actualName(),
                    String.join("\n", content.actualDetails()),
                    String.join("\n", content.categoryResults()),
                    content.leaderboardLink().toASCIIString()
                },
                locale));
        outgoingMailService.send(message);
    }
}

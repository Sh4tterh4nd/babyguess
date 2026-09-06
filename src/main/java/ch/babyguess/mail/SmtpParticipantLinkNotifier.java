package ch.babyguess.mail;

import ch.babyguess.participant.Participant;
import java.net.URI;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;

class SmtpParticipantLinkNotifier implements ParticipantLinkNotifier {

    private final OutgoingMailService outgoingMailService;
    private final MessageSource messageSource;

    SmtpParticipantLinkNotifier(OutgoingMailService outgoingMailService, MessageSource messageSource) {
        this.outgoingMailService = outgoingMailService;
        this.messageSource = messageSource;
    }

    @Override
    public void send(Participant participant, Locale locale, URI editLink) {
        var message = new SimpleMailMessage();
        message.setTo(participant.getEmailAddress());
        message.setSubject(messageSource.getMessage("mail.edit.subject", null, locale));
        message.setText(messageSource.getMessage(
                "mail.edit.body",
                new Object[] {participant.getDisplayName(), editLink.toASCIIString()},
                locale));
        outgoingMailService.send(message);
    }
}

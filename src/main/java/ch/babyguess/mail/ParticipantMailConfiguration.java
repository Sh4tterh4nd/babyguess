package ch.babyguess.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ParticipantMailConfiguration {

    @Bean
    @ConditionalOnMissingBean(ParticipantLinkNotifier.class)
    ParticipantLinkNotifier smtpParticipantLinkNotifier(
            OutgoingMailService outgoingMailService,
            MessageSource messageSource) {
        return new SmtpParticipantLinkNotifier(outgoingMailService, messageSource);
    }

    @Bean
    @ConditionalOnMissingBean(RevealResultNotifier.class)
    RevealResultNotifier smtpRevealResultNotifier(
            OutgoingMailService outgoingMailService,
            MessageSource messageSource) {
        return new SmtpRevealResultNotifier(outgoingMailService, messageSource);
    }
}

package ch.babyguess.mail;

import ch.babyguess.participant.Participant;
import java.util.Locale;

public interface RevealResultNotifier {

    void send(Participant participant, Locale locale, RevealEmailContent content);
}

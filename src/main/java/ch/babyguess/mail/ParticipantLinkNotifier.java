package ch.babyguess.mail;

import ch.babyguess.participant.Participant;
import java.net.URI;
import java.util.Locale;

public interface ParticipantLinkNotifier {

    void send(Participant participant, Locale locale, URI editLink);
}

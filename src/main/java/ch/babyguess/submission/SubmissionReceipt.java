package ch.babyguess.submission;

import ch.babyguess.participant.Participant;
import java.util.UUID;

public record SubmissionReceipt(UUID deliveryId, Participant participant, String rawToken) {

    @Override
    public String toString() {
        return "SubmissionReceipt[deliveryId=" + deliveryId
                + ", participantId=" + participant.getId()
                + ", rawToken=REDACTED]";
    }
}

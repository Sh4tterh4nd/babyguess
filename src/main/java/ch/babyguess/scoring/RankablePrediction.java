package ch.babyguess.scoring;

import java.util.UUID;

public record RankablePrediction(UUID participantId, String displayName, ParticipantPrediction prediction) {

    public RankablePrediction {
        if (participantId == null || displayName == null || displayName.isBlank() || prediction == null) {
            throw new IllegalArgumentException("Participant identity and prediction are required");
        }
    }
}

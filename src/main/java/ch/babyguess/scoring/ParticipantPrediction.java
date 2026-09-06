package ch.babyguess.scoring;

import java.time.LocalDate;
import java.util.List;

public record ParticipantPrediction(
        List<String> nameGuesses,
        Sex sex,
        LocalDate birthDate,
        Integer birthWeightGrams) {

    public ParticipantPrediction {
        nameGuesses = nameGuesses == null ? List.of() : List.copyOf(nameGuesses);
        if (birthWeightGrams != null && birthWeightGrams <= 0) {
            throw new IllegalArgumentException("Predicted birth weight must be positive");
        }
    }
}

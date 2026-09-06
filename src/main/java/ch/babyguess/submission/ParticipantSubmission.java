package ch.babyguess.submission;

import ch.babyguess.scoring.Sex;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public record ParticipantSubmission(
        String displayName,
        String emailAddress,
        List<String> nameGuesses,
        Sex predictedSex,
        LocalDate predictedBirthDate,
        Integer predictedBirthWeightGrams,
        Locale locale) {
}

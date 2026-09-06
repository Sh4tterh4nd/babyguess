package ch.babyguess.scoring;

import java.math.BigDecimal;

public record ScoringConfiguration(
        int maximumNameGuesses,
        boolean rankedNameScoring,
        BigDecimal nameWeight,
        WeightedCategory sex,
        ToleratedCategory birthDate,
        ToleratedCategory birthWeight) {

    public ScoringConfiguration {
        if (maximumNameGuesses < 1) {
            throw new IllegalArgumentException("At least one name guess must be allowed");
        }
        if (nameWeight == null || nameWeight.signum() < 0) {
            throw new IllegalArgumentException("Name weight must be zero or greater");
        }
        if (sex == null || birthDate == null || birthWeight == null) {
            throw new IllegalArgumentException("All category settings are required");
        }
    }
}

package ch.babyguess.event;

import java.math.BigDecimal;
import java.time.Instant;

public record EventConfigurationUpdate(
        long expectedVersion,
        String title,
        Instant submissionDeadline,
        String eventTimezone,
        int maximumNameGuesses,
        boolean rankedNameScoring,
        BigDecimal nameWeight,
        boolean sexEnabled,
        BigDecimal sexWeight,
        boolean birthDateEnabled,
        BigDecimal birthDateWeight,
        int birthDateToleranceDays,
        boolean birthWeightEnabled,
        BigDecimal birthWeightWeight,
        int birthWeightToleranceGrams) {
}

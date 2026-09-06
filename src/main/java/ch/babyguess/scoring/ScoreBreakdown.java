package ch.babyguess.scoring;

import ch.babyguess.name.NameMatchType;
import java.math.BigDecimal;

public record ScoreBreakdown(
        BigDecimal nameScore,
        BigDecimal sexScore,
        BigDecimal birthDateScore,
        BigDecimal birthWeightScore,
        BigDecimal totalScore,
        NameMatchType nameMatchType,
        Integer matchedNamePosition,
        Integer birthDateDifferenceDays,
        Integer birthWeightDifferenceGrams) {
}

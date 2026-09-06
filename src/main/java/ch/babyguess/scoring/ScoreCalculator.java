package ch.babyguess.scoring;

import ch.babyguess.name.NameMatchType;
import ch.babyguess.name.NameMatcher;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ScoreCalculator {

    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final NameMatcher nameMatcher;

    public ScoreCalculator(NameMatcher nameMatcher) {
        this.nameMatcher = nameMatcher;
    }

    public ScoreBreakdown calculate(
            ScoringConfiguration configuration,
            ParticipantPrediction prediction,
            ActualBaby actual) {
        return calculate(configuration, prediction, actual, Set.of());
    }

    public ScoreBreakdown calculate(
            ScoringConfiguration configuration,
            ParticipantPrediction prediction,
            ActualBaby actual,
            Set<String> acceptedCosmeticNameVariants) {
        if (prediction.nameGuesses().size() > configuration.maximumNameGuesses()) {
            throw new IllegalArgumentException("Prediction contains more name guesses than configured");
        }
        var nameResult = scoreName(configuration, prediction, actual, acceptedCosmeticNameVariants);
        var sexScore = scoreSex(configuration.sex(), prediction, actual);

        Integer dateDifference = prediction.birthDate() == null || actual.birthDate() == null
                ? null
                : Math.toIntExact(Math.abs(ChronoUnit.DAYS.between(prediction.birthDate(), actual.birthDate())));
        var dateScore = scoreTolerance(configuration.birthDate(), dateDifference, actual.birthDate() != null);

        Integer weightDifference = prediction.birthWeightGrams() == null || actual.birthWeightGrams() == null
                ? null
                : Math.abs(prediction.birthWeightGrams() - actual.birthWeightGrams());
        var weightScore = scoreTolerance(configuration.birthWeight(), weightDifference,
                actual.birthWeightGrams() != null);

        var total = nameResult.score()
                .add(sexScore)
                .add(dateScore)
                .add(weightScore);
        return new ScoreBreakdown(
                nameResult.score(),
                sexScore,
                dateScore,
                weightScore,
                total,
                nameResult.type(),
                nameResult.position(),
                dateDifference,
                weightDifference);
    }

    private NameScore scoreName(
            ScoringConfiguration configuration,
            ParticipantPrediction prediction,
            ActualBaby actual,
            Set<String> acceptedCosmeticNameVariants) {
        for (int index = 0; index < prediction.nameGuesses().size(); index++) {
            var matchType = nameMatcher.classify(
                    actual.name(), prediction.nameGuesses().get(index), acceptedCosmeticNameVariants);
            if (matchType != NameMatchType.NONE) {
                int position = index + 1;
                var score = configuration.rankedNameScoring()
                        ? configuration.nameWeight().multiply(
                                BigDecimal.valueOf(configuration.maximumNameGuesses() - position + 1)
                                        .divide(BigDecimal.valueOf(configuration.maximumNameGuesses()), PRECISION))
                        : configuration.nameWeight();
                return new NameScore(score, matchType, position);
            }
        }
        return new NameScore(ZERO, NameMatchType.NONE, null);
    }

    private BigDecimal scoreSex(WeightedCategory category, ParticipantPrediction prediction, ActualBaby actual) {
        if (!category.enabled() || actual.sex() == null || prediction.sex() == null) {
            return ZERO;
        }
        return actual.sex() == prediction.sex() ? category.weight() : ZERO;
    }

    private BigDecimal scoreTolerance(ToleratedCategory category, Integer difference, boolean actualIsKnown) {
        if (!category.enabled() || !actualIsKnown || difference == null) {
            return ZERO;
        }
        if (category.tolerance() == 0) {
            return difference == 0 ? category.weight() : ZERO;
        }
        if (difference >= category.tolerance()) {
            return ZERO;
        }
        var factor = BigDecimal.ONE.subtract(
                BigDecimal.valueOf(difference).divide(BigDecimal.valueOf(category.tolerance()), PRECISION));
        return category.weight().multiply(factor);
    }

    private record NameScore(BigDecimal score, NameMatchType type, Integer position) {
    }
}

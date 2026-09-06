package ch.babyguess.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import ch.babyguess.name.NameMatchType;
import ch.babyguess.name.NameMatcher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScoreCalculatorTest {

    private final ScoreCalculator calculator = new ScoreCalculator(new NameMatcher());

    @Test
    void appliesRankingWeightsAndLinearTolerances() {
        var configuration = configuration(true, 3, 5, 500);
        var prediction = new ParticipantPrediction(
                List.of("Noah", "Sara"), Sex.GIRL, LocalDate.of(2026, 10, 11), 3490);
        var actual = new ActualBaby("Sarah", Sex.GIRL, LocalDate.of(2026, 10, 10), 3500);

        var score = calculator.calculate(configuration, prediction, actual);

        assertThat(score.nameMatchType()).isEqualTo(NameMatchType.EQUIVALENT);
        assertThat(score.matchedNamePosition()).isEqualTo(2);
        assertThat(score.nameScore()).isEqualByComparingTo(new BigDecimal("0.6666666666666666666666666666666667"));
        assertThat(score.sexScore()).isEqualByComparingTo("1");
        assertThat(score.birthDateScore()).isEqualByComparingTo("0.8");
        assertThat(score.birthWeightScore()).isEqualByComparingTo("0.98");
        assertThat(score.totalScore()).isEqualByComparingTo("3.4466666666666666666666666666666667");
        assertThat(score.birthDateDifferenceDays()).isEqualTo(1);
        assertThat(score.birthWeightDifferenceGrams()).isEqualTo(10);
    }

    @Test
    void toleranceBoundaryAndMissingValuesScoreZero() {
        var configuration = configuration(false, 3, 5, 500);
        var prediction = new ParticipantPrediction(
                List.of("Other"), null, LocalDate.of(2026, 10, 15), null);
        var actual = new ActualBaby("Sarah", Sex.GIRL, LocalDate.of(2026, 10, 10), 3500);

        var score = calculator.calculate(configuration, prediction, actual);

        assertThat(score.totalScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void zeroToleranceAwardsOnlyAnExactPrediction() {
        var configuration = configuration(false, 3, 0, 0);
        var exact = new ParticipantPrediction(
                List.of("Sarah"), Sex.BOY, LocalDate.of(2026, 10, 10), 3500);
        var actual = new ActualBaby("Sarah", Sex.GIRL, LocalDate.of(2026, 10, 10), 3500);

        var score = calculator.calculate(configuration, exact, actual);

        assertThat(score.nameScore()).isEqualByComparingTo("1");
        assertThat(score.sexScore()).isEqualByComparingTo("0");
        assertThat(score.birthDateScore()).isEqualByComparingTo("1");
        assertThat(score.birthWeightScore()).isEqualByComparingTo("1");
    }

    private ScoringConfiguration configuration(boolean ranked, int names, int days, int grams) {
        return new ScoringConfiguration(
                names,
                ranked,
                BigDecimal.ONE,
                new WeightedCategory(true, BigDecimal.ONE),
                new ToleratedCategory(true, BigDecimal.ONE, days),
                new ToleratedCategory(true, BigDecimal.ONE, grams));
    }
}

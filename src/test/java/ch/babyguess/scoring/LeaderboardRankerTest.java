package ch.babyguess.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import ch.babyguess.name.NameMatchType;
import ch.babyguess.name.NameMatcher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeaderboardRankerTest {

    private final LeaderboardRanker ranker = new LeaderboardRanker(new ScoreCalculator(new NameMatcher()));

    @Test
    void appliesNameClassAndCompetitionPlacesBeforeAlphabeticalPresentation() {
        var participants = List.of(
                participant("Nobody", "Other"),
                participant("Zed", "Sarra"),
                participant("Exact", "Sarah"),
                participant("Amy", "Sara"));

        var result = ranker.rank(configuration(), actual(), Set.of(), participants);

        assertThat(result).extracting(entry -> entry.participant().displayName())
                .containsExactly("Exact", "Amy", "Zed", "Nobody");
        assertThat(result).extracting(RankedPrediction::place).containsExactly(1, 2, 2, 4);
        assertThat(result).extracting(entry -> entry.score().nameMatchType()).containsExactly(
                NameMatchType.EXACT,
                NameMatchType.EQUIVALENT,
                NameMatchType.EQUIVALENT,
                NameMatchType.NONE);
    }

    @Test
    void earlierMatchingGuessBreaksTieEvenWhenNameRankingIsDisabled() {
        var first = new RankablePrediction(
                UUID.randomUUID(), "First", prediction(List.of("Sara", "Other")));
        var second = new RankablePrediction(
                UUID.randomUUID(), "Second", prediction(List.of("Other", "Sara")));

        var result = ranker.rank(configuration(), actual(), Set.of(), List.of(second, first));

        assertThat(result).extracting(entry -> entry.participant().displayName())
                .containsExactly("First", "Second");
        assertThat(result).extracting(RankedPrediction::place).containsExactly(1, 2);
    }

    @Test
    void administratorAcceptedVariantScoresAsEquivalent() {
        var participant = participant("Candidate", "Sarina");

        var result = ranker.rank(configuration(), actual(), Set.of("sarina"), List.of(participant));

        assertThat(result.getFirst().score().nameMatchType()).isEqualTo(NameMatchType.EQUIVALENT);
        assertThat(result.getFirst().score().nameScore()).isEqualByComparingTo(BigDecimal.ONE);
    }

    private RankablePrediction participant(String displayName, String name) {
        return new RankablePrediction(UUID.randomUUID(), displayName, prediction(List.of(name)));
    }

    private ParticipantPrediction prediction(List<String> names) {
        return new ParticipantPrediction(names, null, null, null);
    }

    private ActualBaby actual() {
        return new ActualBaby("Sarah", null, LocalDate.of(2026, 10, 10), null);
    }

    private ScoringConfiguration configuration() {
        return new ScoringConfiguration(
                3,
                false,
                BigDecimal.ONE,
                new WeightedCategory(false, BigDecimal.ONE),
                new ToleratedCategory(false, BigDecimal.ONE, 5),
                new ToleratedCategory(false, BigDecimal.ONE, 500));
    }
}

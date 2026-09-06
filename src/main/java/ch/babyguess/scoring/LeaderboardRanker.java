package ch.babyguess.scoring;

import ch.babyguess.name.NameMatchType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardRanker {

    private final ScoreCalculator scoreCalculator;

    public LeaderboardRanker(ScoreCalculator scoreCalculator) {
        this.scoreCalculator = scoreCalculator;
    }

    public List<RankedPrediction> rank(
            ScoringConfiguration configuration,
            ActualBaby actual,
            Set<String> acceptedCosmeticNameVariants,
            List<RankablePrediction> participants) {
        var scored = participants.stream()
                .map(participant -> new Scored(
                        participant,
                        scoreCalculator.calculate(
                                configuration,
                                participant.prediction(),
                                actual,
                                acceptedCosmeticNameVariants)))
                .sorted(resultComparator())
                .toList();
        var result = new ArrayList<RankedPrediction>();
        Scored previous = null;
        int place = 0;
        for (int index = 0; index < scored.size(); index++) {
            var current = scored.get(index);
            if (previous == null || compareForPlace(previous, current) != 0) {
                place = index + 1;
            }
            result.add(new RankedPrediction(place, current.participant(), current.score()));
            previous = current;
        }
        return List.copyOf(result);
    }

    private Comparator<Scored> resultComparator() {
        return (left, right) -> {
            int ranking = compareForPlace(left, right);
            if (ranking != 0) {
                return ranking;
            }
            int alphabetical = left.participant().displayName().toLowerCase(Locale.ROOT)
                    .compareTo(right.participant().displayName().toLowerCase(Locale.ROOT));
            if (alphabetical != 0) {
                return alphabetical;
            }
            int exactCase = left.participant().displayName().compareTo(right.participant().displayName());
            return exactCase != 0
                    ? exactCase
                    : left.participant().participantId().compareTo(right.participant().participantId());
        };
    }

    private int compareForPlace(Scored left, Scored right) {
        int total = right.score().totalScore().compareTo(left.score().totalScore());
        if (total != 0) {
            return total;
        }
        int nameClass = Integer.compare(
                nameClass(right.score().nameMatchType()),
                nameClass(left.score().nameMatchType()));
        if (nameClass != 0) {
            return nameClass;
        }
        return Integer.compare(
                position(left.score().matchedNamePosition()),
                position(right.score().matchedNamePosition()));
    }

    private int nameClass(NameMatchType type) {
        return switch (type) {
            case EXACT -> 2;
            case EQUIVALENT -> 1;
            case NONE -> 0;
        };
    }

    private int position(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    private record Scored(RankablePrediction participant, ScoreBreakdown score) {
    }
}

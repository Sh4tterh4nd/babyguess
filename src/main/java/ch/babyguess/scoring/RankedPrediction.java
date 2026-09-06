package ch.babyguess.scoring;

public record RankedPrediction(int place, RankablePrediction participant, ScoreBreakdown score) {
}

package ch.babyguess.web.admin;

import ch.babyguess.name.NameMatchDecisionType;
import ch.babyguess.name.NameMatchType;
import ch.babyguess.scoring.Sex;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminRevealView(
        boolean eventClosed,
        String deadline,
        boolean actualDetailsSaved,
        String actualName,
        Sex actualSex,
        LocalDate actualBirthDate,
        Integer actualBirthWeightGrams,
        boolean sexEnabled,
        boolean birthDateEnabled,
        boolean birthWeightEnabled,
        int eligibleParticipantCount,
        List<NameCandidate> candidates,
        List<NameDecision> decisions,
        List<LeaderboardEntry> leaderboard) {

    public record NameCandidate(String displayName, int participantCount) {
    }

    public record NameDecision(String displayName, NameMatchDecisionType decision) {

        public boolean isAccepted() {
            return decision == NameMatchDecisionType.ACCEPTED;
        }
    }

    public record LeaderboardEntry(
            int place,
            UUID participantId,
            String displayName,
            List<String> nameGuesses,
            String matchedName,
            NameMatchType nameMatchType,
            String nameScore,
            String totalScore,
            CategoryResult sex,
            CategoryResult birthDate,
            CategoryResult birthWeight) {

        public boolean isExactName() {
            return nameMatchType == NameMatchType.EXACT;
        }

        public boolean isEquivalentName() {
            return nameMatchType == NameMatchType.EQUIVALENT;
        }
    }

    public record CategoryResult(
            ResultOutcome outcome,
            String prediction,
            String score,
            Integer difference,
            Integer percentage) {

        public boolean isExact() { return outcome == ResultOutcome.EXACT; }
        public boolean isClose() { return outcome == ResultOutcome.CLOSE; }
        public boolean isWrong() { return outcome == ResultOutcome.WRONG; }
        public boolean isMissing() { return outcome == ResultOutcome.MISSING; }
        public boolean isAwaiting() { return outcome == ResultOutcome.AWAITING; }
    }

    public enum ResultOutcome {
        EXACT,
        CLOSE,
        WRONG,
        MISSING,
        AWAITING
    }
}

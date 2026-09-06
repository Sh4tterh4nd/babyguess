package ch.babyguess.web;

import ch.babyguess.name.NameMatchType;
import ch.babyguess.scoring.Sex;
import ch.babyguess.web.admin.AdminRevealView;
import java.time.LocalDate;
import java.util.List;

public record PublicRevealView(
        String eventTitle,
        String actualName,
        Sex actualSex,
        LocalDate actualBirthDate,
        Integer actualBirthWeightGrams,
        boolean sexEnabled,
        boolean birthDateEnabled,
        boolean birthWeightEnabled,
        List<LeaderboardEntry> leaderboard) {

    public record LeaderboardEntry(
            int place,
            String displayName,
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
            AdminRevealView.ResultOutcome outcome,
            String prediction,
            String score,
            Integer difference,
            Integer percentage) {

        public boolean isExact() { return outcome == AdminRevealView.ResultOutcome.EXACT; }
        public boolean isClose() { return outcome == AdminRevealView.ResultOutcome.CLOSE; }
        public boolean isWrong() { return outcome == AdminRevealView.ResultOutcome.WRONG; }
        public boolean isMissing() { return outcome == AdminRevealView.ResultOutcome.MISSING; }
        public boolean isAwaiting() { return outcome == AdminRevealView.ResultOutcome.AWAITING; }
    }
}

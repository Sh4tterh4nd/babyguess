package ch.babyguess.submission;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class EffectiveSubmissionSelectorTest {

    private final EffectiveSubmissionSelector selector = new EffectiveSubmissionSelector();

    @Test
    void selectsNewestVersionAtOrBeforeDeadline() {
        var first = new TimedSubmission<>(Instant.parse("2026-09-01T10:00:00Z"), "first");
        var second = new TimedSubmission<>(Instant.parse("2026-09-02T10:00:00Z"), "second");
        var late = new TimedSubmission<>(Instant.parse("2026-09-03T10:00:00Z"), "late");

        var effective = selector.select(
                List.of(first, late, second), Instant.parse("2026-09-02T10:00:00Z"));

        assertThat(effective).contains(second);
    }

    @Test
    void movingDeadlineBackSelectsAnEarlierVersionWithoutChangingHistory() {
        var history = List.of(
                new TimedSubmission<>(Instant.parse("2026-09-01T10:00:00Z"), "first"),
                new TimedSubmission<>(Instant.parse("2026-09-02T10:00:00Z"), "second"));

        var effective = selector.select(history, Instant.parse("2026-09-01T12:00:00Z"));

        assertThat(effective).get().extracting(TimedSubmission::value).isEqualTo("first");
        assertThat(history).hasSize(2);
    }

    @Test
    void returnsEmptyWhenNoVersionQualified() {
        var history = List.of(new TimedSubmission<>(Instant.parse("2026-09-02T10:00:00Z"), "late"));

        assertThat(selector.select(history, Instant.parse("2026-09-01T10:00:00Z"))).isEmpty();
    }
}

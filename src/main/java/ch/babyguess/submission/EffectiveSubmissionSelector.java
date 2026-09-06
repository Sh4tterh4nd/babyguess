package ch.babyguess.submission;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EffectiveSubmissionSelector {

    public <T> Optional<TimedSubmission<T>> select(Collection<TimedSubmission<T>> history, Instant deadline) {
        if (history == null || deadline == null) {
            throw new IllegalArgumentException("Submission history and deadline are required");
        }
        return history.stream()
                .filter(submission -> !submission.submittedAt().isAfter(deadline))
                .max(Comparator.comparing(TimedSubmission::submittedAt));
    }
}

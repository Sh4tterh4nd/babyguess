package ch.babyguess.submission;

import java.time.Instant;

public record TimedSubmission<T>(Instant submittedAt, T value) {

    public TimedSubmission {
        if (submittedAt == null || value == null) {
            throw new IllegalArgumentException("Submission time and value are required");
        }
    }
}

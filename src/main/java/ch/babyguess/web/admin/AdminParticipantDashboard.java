package ch.babyguess.web.admin;

import ch.babyguess.mail.LinkDeliveryStatus;
import ch.babyguess.scoring.Sex;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminParticipantDashboard(
        String deadline,
        long participantCount,
        long effectiveSubmissionCount,
        long failedDeliveryCount,
        List<ParticipantEntry> participants) {

    public record ParticipantEntry(
            UUID id,
            String displayName,
            String emailAddress,
            String createdAt,
            SubmissionEntry effectiveSubmission,
            List<SubmissionEntry> history,
            DeliveryEntry delivery) {
    }

    public record SubmissionEntry(
            int versionNumber,
            String submittedAt,
            boolean effective,
            boolean afterDeadline,
            List<String> nameGuesses,
            Sex predictedSex,
            LocalDate predictedBirthDate,
            Integer predictedBirthWeightGrams) {
    }

    public record DeliveryEntry(
            LinkDeliveryStatus status,
            int attemptCount,
            String createdAt,
            String lastAttemptAt,
            String failureCode,
            boolean retryable) {

        public boolean isSent() {
            return status == LinkDeliveryStatus.SENT;
        }

        public boolean isFailed() {
            return status == LinkDeliveryStatus.FAILED;
        }

        public boolean isPending() {
            return status == LinkDeliveryStatus.PENDING;
        }
    }
}

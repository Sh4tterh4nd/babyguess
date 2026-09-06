package ch.babyguess.mail;

import ch.babyguess.participant.Participant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reveal_email_delivery")
public class RevealEmailDelivery {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LinkDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failure_code", length = 160)
    private String failureCode;

    protected RevealEmailDelivery() {
    }

    public RevealEmailDelivery(UUID id, Participant participant, Instant createdAt) {
        this.id = id;
        this.participant = participant;
        this.status = LinkDeliveryStatus.PENDING;
        this.createdAt = createdAt;
    }

    public void markSent(Instant now) {
        status = LinkDeliveryStatus.SENT;
        attemptCount++;
        lastAttemptAt = now;
        deliveredAt = now;
        failureCode = null;
    }

    public void markFailed(Instant now, String safeFailureCode) {
        status = LinkDeliveryStatus.FAILED;
        attemptCount++;
        lastAttemptAt = now;
        failureCode = safeFailureCode;
    }

    public UUID getId() { return id; }
    public Participant getParticipant() { return participant; }
    public LinkDeliveryStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public String getFailureCode() { return failureCode; }
}

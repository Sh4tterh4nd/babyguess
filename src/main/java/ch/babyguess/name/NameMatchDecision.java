package ch.babyguess.name;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "name_match_decision",
        uniqueConstraints = @UniqueConstraint(columnNames = {"actual_cosmetic_name", "guessed_cosmetic_name"}))
public class NameMatchDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actual_cosmetic_name", nullable = false, length = 160)
    private String actualCosmeticName;

    @Column(name = "guessed_cosmetic_name", nullable = false, length = 160)
    private String guessedCosmeticName;

    @Column(name = "guessed_display_name", nullable = false, length = 160)
    private String guessedDisplayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NameMatchDecisionType decision;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NameMatchDecision() {
    }

    public NameMatchDecision(
            String actualCosmeticName,
            String guessedCosmeticName,
            String guessedDisplayName,
            NameMatchDecisionType decision,
            Instant updatedAt) {
        this.actualCosmeticName = actualCosmeticName;
        this.guessedCosmeticName = guessedCosmeticName;
        this.guessedDisplayName = guessedDisplayName;
        this.decision = decision;
        this.updatedAt = updatedAt;
    }

    public void update(String displayName, NameMatchDecisionType newDecision, Instant now) {
        guessedDisplayName = displayName;
        decision = newDecision;
        updatedAt = now;
    }

    public Long getId() { return id; }
    public String getActualCosmeticName() { return actualCosmeticName; }
    public String getGuessedCosmeticName() { return guessedCosmeticName; }
    public String getGuessedDisplayName() { return guessedDisplayName; }
    public NameMatchDecisionType getDecision() { return decision; }
    public Instant getUpdatedAt() { return updatedAt; }
}

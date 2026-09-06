package ch.babyguess.submission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "name_guess")
public class NameGuess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_version_id", nullable = false)
    private SubmissionVersion submissionVersion;

    @Column(name = "guess_position", nullable = false)
    private int guessPosition;

    @Column(name = "guessed_name", nullable = false, length = 160)
    private String guessedName;

    @Column(name = "cosmetic_name", nullable = false, length = 160)
    private String cosmeticName;

    protected NameGuess() {
    }

    NameGuess(SubmissionVersion submissionVersion, int guessPosition, String guessedName, String cosmeticName) {
        this.submissionVersion = submissionVersion;
        this.guessPosition = guessPosition;
        this.guessedName = guessedName;
        this.cosmeticName = cosmeticName;
    }

    public Long getId() { return id; }
    public int getGuessPosition() { return guessPosition; }
    public String getGuessedName() { return guessedName; }
    public String getCosmeticName() { return cosmeticName; }
}

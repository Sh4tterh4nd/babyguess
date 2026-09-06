package ch.babyguess.submission;

import ch.babyguess.participant.Participant;
import ch.babyguess.scoring.Sex;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "submission_version")
public class SubmissionVersion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "predicted_sex", length = 16)
    private Sex predictedSex;

    @Column(name = "predicted_birth_date")
    private LocalDate predictedBirthDate;

    @Column(name = "predicted_birth_weight_grams")
    private Integer predictedBirthWeightGrams;

    @OneToMany(mappedBy = "submissionVersion", cascade = CascadeType.ALL)
    @OrderBy("guessPosition ASC")
    private List<NameGuess> nameGuesses = new ArrayList<>();

    protected SubmissionVersion() {
    }

    public SubmissionVersion(
            UUID id,
            Participant participant,
            Instant submittedAt,
            Sex predictedSex,
            LocalDate predictedBirthDate,
            Integer predictedBirthWeightGrams,
            List<SubmittedName> names) {
        this.id = id;
        this.participant = participant;
        this.submittedAt = submittedAt;
        this.predictedSex = predictedSex;
        this.predictedBirthDate = predictedBirthDate;
        this.predictedBirthWeightGrams = predictedBirthWeightGrams;
        for (int index = 0; index < names.size(); index++) {
            var name = names.get(index);
            nameGuesses.add(new NameGuess(this, index + 1, name.value(), name.cosmeticForm()));
        }
    }

    public UUID getId() { return id; }
    public Participant getParticipant() { return participant; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Sex getPredictedSex() { return predictedSex; }
    public LocalDate getPredictedBirthDate() { return predictedBirthDate; }
    public Integer getPredictedBirthWeightGrams() { return predictedBirthWeightGrams; }
    public List<NameGuess> getNameGuesses() { return Collections.unmodifiableList(nameGuesses); }
}

package ch.babyguess.web;

import ch.babyguess.scoring.Sex;
import ch.babyguess.submission.ParticipantSubmission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.format.annotation.DateTimeFormat;

public class SubmissionForm {

    @NotBlank(message = "{validation.required}")
    @Size(max = 120, message = "{validation.displayName.size}")
    private String displayName;

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    @Size(max = 320, message = "{validation.email.size}")
    private String emailAddress;

    private List<@Size(max = 160, message = "{validation.name.size}") String> nameGuesses = new ArrayList<>();

    private Sex predictedSex;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate predictedBirthDate;

    @Positive(message = "{validation.weight.positive}")
    private Integer predictedBirthWeightGrams;

    public void ensureNameSlots(int maximum) {
        if (nameGuesses == null) {
            nameGuesses = new ArrayList<>();
        }
        while (nameGuesses.size() < maximum) {
            nameGuesses.add("");
        }
    }

    public ParticipantSubmission toSubmission(Locale locale) {
        return new ParticipantSubmission(
                displayName,
                emailAddress,
                nameGuesses == null ? List.of() : List.copyOf(nameGuesses),
                predictedSex,
                predictedBirthDate,
                predictedBirthWeightGrams,
                locale);
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    public List<String> getNameGuesses() { return nameGuesses; }
    public void setNameGuesses(List<String> nameGuesses) { this.nameGuesses = nameGuesses; }
    public Sex getPredictedSex() { return predictedSex; }
    public void setPredictedSex(Sex predictedSex) { this.predictedSex = predictedSex; }
    public LocalDate getPredictedBirthDate() { return predictedBirthDate; }
    public void setPredictedBirthDate(LocalDate predictedBirthDate) { this.predictedBirthDate = predictedBirthDate; }
    public Integer getPredictedBirthWeightGrams() { return predictedBirthWeightGrams; }
    public void setPredictedBirthWeightGrams(Integer value) { this.predictedBirthWeightGrams = value; }
}

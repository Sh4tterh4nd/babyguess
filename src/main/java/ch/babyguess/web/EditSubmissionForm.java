package ch.babyguess.web;

import ch.babyguess.scoring.Sex;
import ch.babyguess.submission.EditSubmissionView;
import ch.babyguess.submission.PredictionValues;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

public class EditSubmissionForm {

    private List<@Size(max = 160, message = "{validation.name.size}") String> nameGuesses = new ArrayList<>();
    private Sex predictedSex;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate predictedBirthDate;

    @Positive(message = "{validation.weight.positive}")
    private Integer predictedBirthWeightGrams;

    public static EditSubmissionForm from(EditSubmissionView view, int maximumNameGuesses) {
        var form = new EditSubmissionForm();
        form.nameGuesses.addAll(view.nameGuesses());
        form.predictedSex = view.predictedSex();
        form.predictedBirthDate = view.predictedBirthDate();
        form.predictedBirthWeightGrams = view.predictedBirthWeightGrams();
        form.ensureNameSlots(maximumNameGuesses);
        return form;
    }

    public void ensureNameSlots(int maximum) {
        if (nameGuesses == null) {
            nameGuesses = new ArrayList<>();
        }
        while (nameGuesses.size() < maximum) {
            nameGuesses.add("");
        }
    }

    public PredictionValues toPredictionValues() {
        return new PredictionValues(
                nameGuesses == null ? List.of() : List.copyOf(nameGuesses),
                predictedSex,
                predictedBirthDate,
                predictedBirthWeightGrams);
    }

    public List<String> getNameGuesses() { return nameGuesses; }
    public void setNameGuesses(List<String> nameGuesses) { this.nameGuesses = nameGuesses; }
    public Sex getPredictedSex() { return predictedSex; }
    public void setPredictedSex(Sex predictedSex) { this.predictedSex = predictedSex; }
    public LocalDate getPredictedBirthDate() { return predictedBirthDate; }
    public void setPredictedBirthDate(LocalDate predictedBirthDate) { this.predictedBirthDate = predictedBirthDate; }
    public Integer getPredictedBirthWeightGrams() { return predictedBirthWeightGrams; }
    public void setPredictedBirthWeightGrams(Integer value) { this.predictedBirthWeightGrams = value; }
}

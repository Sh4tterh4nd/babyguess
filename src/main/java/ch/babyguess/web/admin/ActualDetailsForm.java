package ch.babyguess.web.admin;

import ch.babyguess.event.EventConfiguration;
import ch.babyguess.scoring.ActualBaby;
import ch.babyguess.scoring.Sex;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class ActualDetailsForm {

    private long version;

    @NotBlank(message = "{validation.required}")
    @Size(max = 160, message = "{validation.name.size}")
    private String actualName;

    private Sex actualSex;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate actualBirthDate;

    @Positive(message = "{validation.weight.positive}")
    private Integer actualBirthWeightGrams;

    public static ActualDetailsForm from(EventConfiguration event) {
        var form = new ActualDetailsForm();
        form.version = event.getVersion();
        form.actualName = event.getActualName();
        form.actualSex = event.getActualSex();
        form.actualBirthDate = event.getActualBirthDate();
        form.actualBirthWeightGrams = event.getActualBirthWeightGrams();
        return form;
    }

    public ActualBaby toActualBaby() {
        return new ActualBaby(actualName.strip(), actualSex, actualBirthDate, actualBirthWeightGrams);
    }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public String getActualName() { return actualName; }
    public void setActualName(String actualName) { this.actualName = actualName; }
    public Sex getActualSex() { return actualSex; }
    public void setActualSex(Sex actualSex) { this.actualSex = actualSex; }
    public LocalDate getActualBirthDate() { return actualBirthDate; }
    public void setActualBirthDate(LocalDate actualBirthDate) { this.actualBirthDate = actualBirthDate; }
    public Integer getActualBirthWeightGrams() { return actualBirthWeightGrams; }
    public void setActualBirthWeightGrams(Integer actualBirthWeightGrams) {
        this.actualBirthWeightGrams = actualBirthWeightGrams;
    }
}

package ch.babyguess.web.admin;

import ch.babyguess.event.EventConfiguration;
import ch.babyguess.event.EventConfigurationUpdate;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.format.annotation.DateTimeFormat;

public class EventConfigurationForm {

    private long version;

    @NotBlank(message = "{validation.required}")
    @Size(max = 160, message = "{validation.title.size}")
    private String title;

    @NotNull(message = "{validation.required}")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime submissionDeadline;

    @NotBlank(message = "{validation.required}")
    @Size(max = 80, message = "{validation.timezone.size}")
    private String eventTimezone;

    @Min(value = 1, message = "{validation.minimum.one}")
    @Max(value = 20, message = "{validation.maximum.names}")
    private int maximumNameGuesses;

    private boolean rankedNameScoring;

    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0", message = "{validation.nonnegative}")
    @Digits(integer = 11, fraction = 8, message = "{validation.weight.digits}")
    private BigDecimal nameWeight;

    private boolean sexEnabled;

    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0", message = "{validation.nonnegative}")
    @Digits(integer = 11, fraction = 8, message = "{validation.weight.digits}")
    private BigDecimal sexWeight;

    private boolean birthDateEnabled;

    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0", message = "{validation.nonnegative}")
    @Digits(integer = 11, fraction = 8, message = "{validation.weight.digits}")
    private BigDecimal birthDateWeight;

    @Min(value = 0, message = "{validation.nonnegative}")
    private int birthDateToleranceDays;

    private boolean birthWeightEnabled;

    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0", message = "{validation.nonnegative}")
    @Digits(integer = 11, fraction = 8, message = "{validation.weight.digits}")
    private BigDecimal birthWeightWeight;

    @Min(value = 0, message = "{validation.nonnegative}")
    private int birthWeightToleranceGrams;

    public EventConfigurationForm() {
    }

    public static EventConfigurationForm from(EventConfiguration configuration) {
        var form = new EventConfigurationForm();
        var zone = ZoneId.of(configuration.getEventTimezone());
        form.version = configuration.getVersion();
        form.title = configuration.getTitle();
        form.submissionDeadline = configuration.getSubmissionDeadline() == null
                ? null
                : LocalDateTime.ofInstant(configuration.getSubmissionDeadline(), zone)
                        .truncatedTo(ChronoUnit.MINUTES);
        form.eventTimezone = configuration.getEventTimezone();
        form.maximumNameGuesses = configuration.getMaximumNameGuesses();
        form.rankedNameScoring = configuration.isRankedNameScoring();
        form.nameWeight = configuration.getNameWeight();
        form.sexEnabled = configuration.isSexEnabled();
        form.sexWeight = configuration.getSexWeight();
        form.birthDateEnabled = configuration.isBirthDateEnabled();
        form.birthDateWeight = configuration.getBirthDateWeight();
        form.birthDateToleranceDays = configuration.getBirthDateToleranceDays();
        form.birthWeightEnabled = configuration.isBirthWeightEnabled();
        form.birthWeightWeight = configuration.getBirthWeightWeight();
        form.birthWeightToleranceGrams = configuration.getBirthWeightToleranceGrams();
        return form;
    }

    @AssertTrue(message = "{validation.timezone.invalid}")
    public boolean isTimezoneValid() {
        if (eventTimezone == null || eventTimezone.isBlank()) {
            return true;
        }
        try {
            ZoneId.of(eventTimezone);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @AssertTrue(message = "{validation.deadline.dst}")
    public boolean isDeadlineResolvable() {
        if (submissionDeadline == null || !isTimezoneValid()) {
            return true;
        }
        return ZoneId.of(eventTimezone).getRules().getValidOffsets(submissionDeadline).size() == 1;
    }

    public EventConfigurationUpdate toUpdate() {
        var zone = ZoneId.of(eventTimezone);
        return new EventConfigurationUpdate(
                version,
                title.strip(),
                submissionDeadline.atZone(zone).toInstant(),
                zone.getId(),
                maximumNameGuesses,
                rankedNameScoring,
                nameWeight,
                sexEnabled,
                sexWeight,
                birthDateEnabled,
                birthDateWeight,
                birthDateToleranceDays,
                birthWeightEnabled,
                birthWeightWeight,
                birthWeightToleranceGrams);
    }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getSubmissionDeadline() { return submissionDeadline; }
    public void setSubmissionDeadline(LocalDateTime submissionDeadline) { this.submissionDeadline = submissionDeadline; }
    public String getEventTimezone() { return eventTimezone; }
    public void setEventTimezone(String eventTimezone) { this.eventTimezone = eventTimezone; }
    public int getMaximumNameGuesses() { return maximumNameGuesses; }
    public void setMaximumNameGuesses(int maximumNameGuesses) { this.maximumNameGuesses = maximumNameGuesses; }
    public boolean isRankedNameScoring() { return rankedNameScoring; }
    public void setRankedNameScoring(boolean rankedNameScoring) { this.rankedNameScoring = rankedNameScoring; }
    public BigDecimal getNameWeight() { return nameWeight; }
    public void setNameWeight(BigDecimal nameWeight) { this.nameWeight = nameWeight; }
    public boolean isSexEnabled() { return sexEnabled; }
    public void setSexEnabled(boolean sexEnabled) { this.sexEnabled = sexEnabled; }
    public BigDecimal getSexWeight() { return sexWeight; }
    public void setSexWeight(BigDecimal sexWeight) { this.sexWeight = sexWeight; }
    public boolean isBirthDateEnabled() { return birthDateEnabled; }
    public void setBirthDateEnabled(boolean birthDateEnabled) { this.birthDateEnabled = birthDateEnabled; }
    public BigDecimal getBirthDateWeight() { return birthDateWeight; }
    public void setBirthDateWeight(BigDecimal birthDateWeight) { this.birthDateWeight = birthDateWeight; }
    public int getBirthDateToleranceDays() { return birthDateToleranceDays; }
    public void setBirthDateToleranceDays(int value) { this.birthDateToleranceDays = value; }
    public boolean isBirthWeightEnabled() { return birthWeightEnabled; }
    public void setBirthWeightEnabled(boolean birthWeightEnabled) { this.birthWeightEnabled = birthWeightEnabled; }
    public BigDecimal getBirthWeightWeight() { return birthWeightWeight; }
    public void setBirthWeightWeight(BigDecimal birthWeightWeight) { this.birthWeightWeight = birthWeightWeight; }
    public int getBirthWeightToleranceGrams() { return birthWeightToleranceGrams; }
    public void setBirthWeightToleranceGrams(int value) { this.birthWeightToleranceGrams = value; }
}

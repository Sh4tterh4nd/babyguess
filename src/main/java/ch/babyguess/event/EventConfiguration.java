package ch.babyguess.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import ch.babyguess.scoring.ActualBaby;
import ch.babyguess.scoring.Sex;

@Entity
@Table(name = "event_configuration")
public class EventConfiguration {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    @Version
    private long version;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "submission_deadline")
    private Instant submissionDeadline;

    @Column(name = "event_timezone", nullable = false, length = 80)
    private String eventTimezone;

    @Column(name = "maximum_name_guesses", nullable = false)
    private int maximumNameGuesses;

    @Column(name = "ranked_name_scoring", nullable = false)
    private boolean rankedNameScoring;

    @Column(name = "name_weight", nullable = false, precision = 19, scale = 8)
    private BigDecimal nameWeight;

    @Column(name = "sex_enabled", nullable = false)
    private boolean sexEnabled;

    @Column(name = "sex_weight", nullable = false, precision = 19, scale = 8)
    private BigDecimal sexWeight;

    @Column(name = "birth_date_enabled", nullable = false)
    private boolean birthDateEnabled;

    @Column(name = "birth_date_weight", nullable = false, precision = 19, scale = 8)
    private BigDecimal birthDateWeight;

    @Column(name = "birth_date_tolerance_days", nullable = false)
    private int birthDateToleranceDays;

    @Column(name = "birth_weight_enabled", nullable = false)
    private boolean birthWeightEnabled;

    @Column(name = "birth_weight_weight", nullable = false, precision = 19, scale = 8)
    private BigDecimal birthWeightWeight;

    @Column(name = "birth_weight_tolerance_grams", nullable = false)
    private int birthWeightToleranceGrams;

    @Column(name = "logo_asset_name", length = 255)
    private String logoAssetName;

    @Column(name = "background_asset_name", length = 255)
    private String backgroundAssetName;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "actual_name", length = 160)
    private String actualName;

    @Column(name = "actual_sex", length = 16)
    @Enumerated(EnumType.STRING)
    private Sex actualSex;

    @Column(name = "actual_birth_date")
    private LocalDate actualBirthDate;

    @Column(name = "actual_birth_weight_grams")
    private Integer actualBirthWeightGrams;

    @Column(name = "revealed_at")
    private Instant revealedAt;

    protected EventConfiguration() {
    }

    public void update(EventConfigurationUpdate update, Instant now) {
        title = update.title();
        submissionDeadline = update.submissionDeadline();
        eventTimezone = update.eventTimezone();
        maximumNameGuesses = update.maximumNameGuesses();
        rankedNameScoring = update.rankedNameScoring();
        nameWeight = update.nameWeight();
        sexEnabled = update.sexEnabled();
        sexWeight = update.sexWeight();
        birthDateEnabled = update.birthDateEnabled();
        birthDateWeight = update.birthDateWeight();
        birthDateToleranceDays = update.birthDateToleranceDays();
        birthWeightEnabled = update.birthWeightEnabled();
        birthWeightWeight = update.birthWeightWeight();
        birthWeightToleranceGrams = update.birthWeightToleranceGrams();
        updatedAt = now;
    }

    public void updateActualDetails(ActualBaby actual, Instant now) {
        actualName = actual.name().strip();
        actualSex = actual.sex();
        actualBirthDate = actual.birthDate();
        actualBirthWeightGrams = actual.birthWeightGrams();
        updatedAt = now;
    }

    public void updateLogoAssetName(String assetName, Instant now) {
        logoAssetName = assetName;
        updatedAt = now;
    }

    public void updateBackgroundAssetName(String assetName, Instant now) {
        backgroundAssetName = assetName;
        updatedAt = now;
    }

    public void markRevealed(Instant now) {
        if (revealedAt == null) {
            revealedAt = now;
            updatedAt = now;
        }
    }

    public Short getId() { return id; }
    public long getVersion() { return version; }
    public String getTitle() { return title; }
    public Instant getSubmissionDeadline() { return submissionDeadline; }
    public String getEventTimezone() { return eventTimezone; }
    public int getMaximumNameGuesses() { return maximumNameGuesses; }
    public boolean isRankedNameScoring() { return rankedNameScoring; }
    public BigDecimal getNameWeight() { return nameWeight; }
    public boolean isSexEnabled() { return sexEnabled; }
    public BigDecimal getSexWeight() { return sexWeight; }
    public boolean isBirthDateEnabled() { return birthDateEnabled; }
    public BigDecimal getBirthDateWeight() { return birthDateWeight; }
    public int getBirthDateToleranceDays() { return birthDateToleranceDays; }
    public boolean isBirthWeightEnabled() { return birthWeightEnabled; }
    public BigDecimal getBirthWeightWeight() { return birthWeightWeight; }
    public int getBirthWeightToleranceGrams() { return birthWeightToleranceGrams; }
    public String getLogoAssetName() { return logoAssetName; }
    public String getBackgroundAssetName() { return backgroundAssetName; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getActualName() { return actualName; }
    public Sex getActualSex() { return actualSex; }
    public LocalDate getActualBirthDate() { return actualBirthDate; }
    public Integer getActualBirthWeightGrams() { return actualBirthWeightGrams; }
    public Instant getRevealedAt() { return revealedAt; }
}

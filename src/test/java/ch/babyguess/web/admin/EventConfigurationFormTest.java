package ch.babyguess.web.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EventConfigurationFormTest {

    @Test
    void rejectsNonexistentDaylightSavingTime() {
        var form = formAt(LocalDateTime.of(2026, 3, 29, 2, 30), "Europe/Zurich");

        assertThat(form.isTimezoneValid()).isTrue();
        assertThat(form.isDeadlineResolvable()).isFalse();
    }

    @Test
    void rejectsAmbiguousDaylightSavingTime() {
        var form = formAt(LocalDateTime.of(2026, 10, 25, 2, 30), "Europe/Zurich");

        assertThat(form.isDeadlineResolvable()).isFalse();
    }

    @Test
    void acceptsOrdinaryLocalTime() {
        var form = formAt(LocalDateTime.of(2026, 10, 1, 12, 30), "Europe/Zurich");

        assertThat(form.isDeadlineResolvable()).isTrue();
        assertThat(form.toUpdate().submissionDeadline()).hasToString("2026-10-01T10:30:00Z");
    }

    private EventConfigurationForm formAt(LocalDateTime deadline, String timezone) {
        var form = new EventConfigurationForm();
        form.setTitle("BabyGuess");
        form.setSubmissionDeadline(deadline);
        form.setEventTimezone(timezone);
        form.setMaximumNameGuesses(3);
        form.setRankedNameScoring(true);
        form.setNameWeight(BigDecimal.ONE);
        form.setSexEnabled(true);
        form.setSexWeight(BigDecimal.ONE);
        form.setBirthDateEnabled(true);
        form.setBirthDateWeight(BigDecimal.ONE);
        form.setBirthDateToleranceDays(5);
        form.setBirthWeightEnabled(true);
        form.setBirthWeightWeight(BigDecimal.ONE);
        form.setBirthWeightToleranceGrams(500);
        return form;
    }
}

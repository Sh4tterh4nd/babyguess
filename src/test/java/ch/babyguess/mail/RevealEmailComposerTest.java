package ch.babyguess.mail;

import static org.assertj.core.api.Assertions.assertThat;

import ch.babyguess.name.NameMatchType;
import ch.babyguess.scoring.Sex;
import ch.babyguess.web.admin.AdminRevealView;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class RevealEmailComposerTest {

    @Test
    void composesLocalizedCategoryResultsWithoutOverallScoreOrPlace() {
        var messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        var composer = new RevealEmailComposer(messages);
        var reveal = reveal();
        var entry = reveal.leaderboard().getFirst();

        var english = composer.compose(reveal, entry, Locale.ENGLISH, URI.create("https://example.test/results"));
        var german = composer.compose(reveal, entry, Locale.GERMAN, URI.create("https://example.test/results?lang=de"));

        assertThat(english.categoryResults()).anyMatch(line -> line.contains("correct"));
        assertThat(english.categoryResults()).anyMatch(line -> line.contains("days off"));
        assertThat(german.categoryResults()).anyMatch(line -> line.contains("richtig"));
        assertThat(german.categoryResults()).anyMatch(line -> line.contains("Tage daneben"));
        assertThat(german.actualDetails()).anyMatch(line -> line.startsWith("Geburtsdatum:"));
        assertThat(german.leaderboardLink().toString()).endsWith("lang=de");

        assertThat(Arrays.stream(RevealEmailContent.class.getRecordComponents())
                        .map(component -> component.getName()))
                .containsExactly("actualName", "actualDetails", "categoryResults", "leaderboardLink")
                .doesNotContain("totalScore", "place");
    }

    private AdminRevealView reveal() {
        var exact = new AdminRevealView.CategoryResult(
                AdminRevealView.ResultOutcome.EXACT, "GIRL", "1", null, null);
        var closeDate = new AdminRevealView.CategoryResult(
                AdminRevealView.ResultOutcome.CLOSE, "2026-10-12", "0.6", 2, 60);
        var closeWeight = new AdminRevealView.CategoryResult(
                AdminRevealView.ResultOutcome.CLOSE, "3400 g", "0.8", 100, 80);
        var entry = new AdminRevealView.LeaderboardEntry(
                1,
                UUID.randomUUID(),
                "Alice",
                List.of("Sarah"),
                "Sarah",
                NameMatchType.EXACT,
                "1",
                "3.4",
                exact,
                closeDate,
                closeWeight);
        return new AdminRevealView(
                true,
                "1 October 2026",
                true,
                "Sarah",
                Sex.GIRL,
                LocalDate.of(2026, 10, 10),
                3500,
                true,
                true,
                true,
                1,
                List.of(),
                List.of(),
                List.of(entry));
    }
}

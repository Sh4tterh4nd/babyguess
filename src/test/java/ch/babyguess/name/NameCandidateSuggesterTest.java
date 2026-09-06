package ch.babyguess.name;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameCandidateSuggesterTest {

    private final NameCandidateSuggester suggester = new NameCandidateSuggester(new NameMatcher());

    @Test
    void suggestsCloseUnmatchedSpellingsOnly() {
        assertThat(suggester.isPlausible("Sarah", "Saria")).isTrue();
        assertThat(suggester.isPlausible("Sarah", "Mara")).isFalse();
        assertThat(suggester.isPlausible("Sarah", "Christopher")).isFalse();
    }

    @Test
    void doesNotSuggestAlreadyAutomaticMatches() {
        assertThat(suggester.isPlausible("Sarah", "Sara")).isFalse();
        assertThat(suggester.isPlausible("Rebecca", "Rebeka")).isFalse();
    }
}

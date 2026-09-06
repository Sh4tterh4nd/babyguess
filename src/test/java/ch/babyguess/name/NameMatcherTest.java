package ch.babyguess.name;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameMatcherTest {

    private final NameMatcher matcher = new NameMatcher();

    @Test
    void cosmeticDifferencesRemainExact() {
        assertThat(matcher.classify("  José  Luis ", "jose\u0301   luis"))
                .isEqualTo(NameMatchType.EXACT);
    }

    @Test
    void requiredAutomaticVariantsAreEquivalent() {
        assertThat(matcher.classify("Sarah", "Sara")).isEqualTo(NameMatchType.EQUIVALENT);
        assertThat(matcher.classify("Rebecca", "Rebeka")).isEqualTo(NameMatchType.EQUIVALENT);
    }

    @Test
    void doubledLettersAndPhAreEquivalent() {
        assertThat(matcher.classify("Philippe", "Filipe")).isEqualTo(NameMatchType.EQUIVALENT);
    }

    @Test
    void softCIsNotBlindlyTreatedAsK() {
        assertThat(matcher.classify("Cindy", "Kindy")).isEqualTo(NameMatchType.NONE);
    }

    @Test
    void unrelatedNamesDoNotMatch() {
        assertThat(matcher.classify("Sara", "Sarina")).isEqualTo(NameMatchType.NONE);
        assertThat(matcher.classify("", "Sara")).isEqualTo(NameMatchType.NONE);
    }
}

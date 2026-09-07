package ch.babyguess.config;

import java.util.List;
import java.util.Locale;

/**
 * The languages BabyGuess ships message bundles for. English is the fallback for anything else, as
 * REQUIREMENTS section 13 requires.
 */
public final class SupportedLanguages {

    public static final Locale FALLBACK = Locale.ENGLISH;

    /** Presentation order in the language selector. */
    public static final List<Locale> ALL = List.of(
            Locale.ENGLISH,
            Locale.GERMAN,
            Locale.forLanguageTag("es"),
            Locale.forLanguageTag("pt"));

    private SupportedLanguages() {
    }

    /**
     * Reduces a locale to the supported language it belongs to, so that regional variants such as
     * {@code de-CH} or {@code pt-BR} resolve to their base language rather than to English.
     */
    public static Locale resolve(Locale locale) {
        if (locale == null) {
            return FALLBACK;
        }
        return ALL.stream()
                .filter(supported -> supported.getLanguage().equals(locale.getLanguage()))
                .findFirst()
                .orElse(FALLBACK);
    }

    public static boolean isSupported(Locale locale) {
        return locale != null && ALL.stream()
                .anyMatch(supported -> supported.getLanguage().equals(locale.getLanguage()));
    }
}

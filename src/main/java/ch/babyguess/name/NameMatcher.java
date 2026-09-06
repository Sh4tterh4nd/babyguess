package ch.babyguess.name;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class NameMatcher {

    private static final Pattern INTERNAL_WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NAME_SEPARATORS = Pattern.compile("[\\s\\-'’]+");

    public NameMatchType classify(String actualName, String guessedName) {
        return classify(actualName, guessedName, Set.of());
    }

    public NameMatchType classify(String actualName, String guessedName, Set<String> acceptedCosmeticVariants) {
        var actual = cosmeticForm(actualName);
        var guess = cosmeticForm(guessedName);
        if (actual.isEmpty() || guess.isEmpty()) {
            return NameMatchType.NONE;
        }
        if (actual.equals(guess)) {
            return NameMatchType.EXACT;
        }
        if (equivalenceKey(actual).equals(equivalenceKey(guess))) {
            return NameMatchType.EQUIVALENT;
        }
        return acceptedCosmeticVariants != null && acceptedCosmeticVariants.contains(guess)
                ? NameMatchType.EQUIVALENT
                : NameMatchType.NONE;
    }

    public String cosmeticForm(String name) {
        if (name == null) {
            return "";
        }
        var normalized = Normalizer.normalize(name, Normalizer.Form.NFKC)
                .strip()
                .toLowerCase(Locale.ROOT);
        return INTERNAL_WHITESPACE.matcher(normalized).replaceAll(" ");
    }

    String equivalenceKey(String cosmeticName) {
        var compact = NAME_SEPARATORS.matcher(cosmeticName).replaceAll("");
        var phonemeNormalized = normalizeLatinPhonemes(compact);
        if (phonemeNormalized.endsWith("h") && phonemeNormalized.length() > 1
                && isVowel(phonemeNormalized.charAt(phonemeNormalized.length() - 2))) {
            phonemeNormalized = phonemeNormalized.substring(0, phonemeNormalized.length() - 1);
        }
        return collapseRepeatedCodePoints(phonemeNormalized);
    }

    private String normalizeLatinPhonemes(String value) {
        var withF = value.replace("ph", "f");
        var result = new StringBuilder(withF.length());
        for (int index = 0; index < withF.length(); index++) {
            char current = withF.charAt(index);
            if (current == 'c' && !isSoftC(withF, index)) {
                result.append('k');
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }

    private boolean isSoftC(String value, int index) {
        if (index + 1 >= value.length()) {
            return false;
        }
        char next = value.charAt(index + 1);
        return next == 'e' || next == 'i' || next == 'y';
    }

    private boolean isVowel(char value) {
        return value == 'a' || value == 'e' || value == 'i' || value == 'o' || value == 'u';
    }

    private String collapseRepeatedCodePoints(String value) {
        var result = new StringBuilder(value.length());
        int previous = -1;
        for (int codePoint : value.codePoints().toArray()) {
            if (codePoint != previous) {
                result.appendCodePoint(codePoint);
                previous = codePoint;
            }
        }
        return result.toString();
    }
}

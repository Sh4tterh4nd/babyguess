package ch.babyguess.name;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class NameCandidateSuggester {

    private static final Pattern NON_LETTERS = Pattern.compile("[^\\p{L}]");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private final NameMatcher matcher;

    public NameCandidateSuggester(NameMatcher matcher) {
        this.matcher = matcher;
    }

    public boolean isPlausible(String actualName, String guessedName) {
        if (matcher.classify(actualName, guessedName) != NameMatchType.NONE) {
            return false;
        }
        var actual = comparisonForm(actualName);
        var guess = comparisonForm(guessedName);
        if (actual.length() < 3 || guess.length() < 3 || actual.charAt(0) != guess.charAt(0)) {
            return false;
        }
        int longest = Math.max(actual.length(), guess.length());
        int threshold = longest <= 4 ? 1 : longest <= 8 ? 2 : 3;
        return Math.abs(actual.length() - guess.length()) <= threshold
                && levenshteinDistance(actual, guess) <= threshold;
    }

    private String comparisonForm(String value) {
        var decomposed = Normalizer.normalize(matcher.cosmeticForm(value), Normalizer.Form.NFKD);
        var withoutMarks = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        return NON_LETTERS.matcher(withoutMarks).replaceAll("").toLowerCase(Locale.ROOT);
    }

    private int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) {
            previous[column] = column;
        }
        for (int row = 1; row <= left.length(); row++) {
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int substitution = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
                current[column] = Math.min(
                        Math.min(current[column - 1] + 1, previous[column] + 1),
                        previous[column - 1] + substitution);
            }
            var swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }
}

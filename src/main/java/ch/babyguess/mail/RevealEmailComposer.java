package ch.babyguess.mail;

import ch.babyguess.web.admin.AdminRevealView;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class RevealEmailComposer {

    private final MessageSource messages;

    public RevealEmailComposer(MessageSource messages) {
        this.messages = messages;
    }

    public RevealEmailContent compose(
            AdminRevealView reveal,
            AdminRevealView.LeaderboardEntry entry,
            Locale locale,
            URI leaderboardLink) {
        var actualDetails = new ArrayList<String>();
        if (reveal.sexEnabled() && reveal.actualSex() != null) {
            actualDetails.add(message(locale, "mail.reveal.actual.sex", sex(reveal.actualSex().name(), locale)));
        }
        if (reveal.birthDateEnabled() && reveal.actualBirthDate() != null) {
            var date = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                    .withLocale(locale)
                    .format(reveal.actualBirthDate());
            actualDetails.add(message(locale, "mail.reveal.actual.date", date));
        }
        if (reveal.birthWeightEnabled() && reveal.actualBirthWeightGrams() != null) {
            actualDetails.add(message(
                    locale, "mail.reveal.actual.weight", reveal.actualBirthWeightGrams()));
        }

        var results = new ArrayList<String>();
        if (entry.isExactName()) {
            results.add(message(locale, "mail.reveal.result.name.exact", entry.matchedName(), entry.nameScore()));
        } else if (entry.isEquivalentName()) {
            results.add(message(
                    locale, "mail.reveal.result.name.equivalent", entry.matchedName(), entry.nameScore()));
        } else {
            results.add(message(locale, "mail.reveal.result.name.none", entry.nameScore()));
        }
        if (reveal.sexEnabled()) {
            results.add(sexResult(entry.sex(), locale));
        }
        if (reveal.birthDateEnabled()) {
            results.add(toleratedResult(entry.birthDate(), "date", locale));
        }
        if (reveal.birthWeightEnabled()) {
            results.add(toleratedResult(entry.birthWeight(), "weight", locale));
        }
        return new RevealEmailContent(reveal.actualName(), actualDetails, results, leaderboardLink);
    }

    private String sexResult(AdminRevealView.CategoryResult result, Locale locale) {
        if (result.isAwaiting()) {
            return message(locale, "mail.reveal.result.sex.awaiting", result.score());
        }
        if (result.isMissing()) {
            return message(locale, "mail.reveal.result.sex.missing", result.score());
        }
        var prediction = sex(result.prediction(), locale);
        var key = result.isExact() ? "mail.reveal.result.sex.exact" : "mail.reveal.result.sex.wrong";
        return message(locale, key, prediction, result.score());
    }

    private String toleratedResult(AdminRevealView.CategoryResult result, String category, Locale locale) {
        var prefix = "mail.reveal.result." + category + ".";
        if (result.isAwaiting()) {
            return message(locale, prefix + "awaiting", result.score());
        }
        if (result.isMissing()) {
            return message(locale, prefix + "missing", result.score());
        }
        if (result.isExact()) {
            return message(locale, prefix + "exact", result.prediction(), result.score());
        }
        if (result.isClose()) {
            return message(
                    locale, prefix + "close", result.prediction(), result.difference(), result.score());
        }
        return message(locale, prefix + "wrong", result.prediction(), result.difference(), result.score());
    }

    private String sex(String sex, Locale locale) {
        return message(locale, "submission.sex." + sex.toLowerCase(Locale.ROOT));
    }

    private String message(Locale locale, String code, Object... arguments) {
        return messages.getMessage(code, arguments, locale);
    }
}

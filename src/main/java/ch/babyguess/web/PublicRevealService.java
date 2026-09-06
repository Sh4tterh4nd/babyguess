package ch.babyguess.web;

import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.web.admin.AdminRevealService;
import ch.babyguess.web.admin.AdminRevealView;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicRevealService {

    private final EventConfigurationService eventService;
    private final AdminRevealService revealService;

    public PublicRevealService(EventConfigurationService eventService, AdminRevealService revealService) {
        this.eventService = eventService;
        this.revealService = revealService;
    }

    @Transactional(readOnly = true)
    public PublicRevealView view(Locale locale) {
        var event = eventService.get();
        if (event.getRevealedAt() == null) {
            throw new RevealNotPublishedException();
        }
        var reveal = revealService.view(locale);
        return new PublicRevealView(
                event.getTitle(),
                reveal.actualName(),
                reveal.actualSex(),
                reveal.actualBirthDate(),
                reveal.actualBirthWeightGrams(),
                reveal.sexEnabled(),
                reveal.birthDateEnabled(),
                reveal.birthWeightEnabled(),
                reveal.leaderboard().stream().map(this::entry).toList());
    }

    private PublicRevealView.LeaderboardEntry entry(AdminRevealView.LeaderboardEntry source) {
        return new PublicRevealView.LeaderboardEntry(
                source.place(),
                source.displayName(),
                source.matchedName(),
                source.nameMatchType(),
                source.nameScore(),
                source.totalScore(),
                result(source.sex()),
                result(source.birthDate()),
                result(source.birthWeight()));
    }

    private PublicRevealView.CategoryResult result(AdminRevealView.CategoryResult source) {
        return new PublicRevealView.CategoryResult(
                source.outcome(),
                source.prediction(),
                source.score(),
                source.difference(),
                source.percentage());
    }
}

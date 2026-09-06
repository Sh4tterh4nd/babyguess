package ch.babyguess.mail;

import ch.babyguess.config.PublicUrlProperties;
import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.web.admin.AdminRevealService;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class RevealEmailDeliveryService {

    private final RevealEmailDeliveryRepository repository;
    private final RevealResultNotifier notifier;
    private final RevealEmailDeliveryRecorder recorder;
    private final RevealEmailComposer composer;
    private final AdminRevealService revealService;
    private final EventConfigurationService eventService;
    private final PublicUrlProperties publicUrl;

    public RevealEmailDeliveryService(
            RevealEmailDeliveryRepository repository,
            RevealResultNotifier notifier,
            RevealEmailDeliveryRecorder recorder,
            RevealEmailComposer composer,
            AdminRevealService revealService,
            EventConfigurationService eventService,
            PublicUrlProperties publicUrl) {
        this.repository = repository;
        this.notifier = notifier;
        this.recorder = recorder;
        this.composer = composer;
        this.revealService = revealService;
        this.eventService = eventService;
        this.publicUrl = publicUrl;
    }

    public LinkDeliveryStatus deliver(UUID deliveryId) {
        var delivery = repository.findWithParticipantById(deliveryId).orElseThrow();
        if (delivery.getStatus() == LinkDeliveryStatus.SENT) {
            return LinkDeliveryStatus.SENT;
        }
        try {
            if (eventService.get().getRevealedAt() == null) {
                throw new IllegalStateException("Reveal is not published");
            }
            var participant = delivery.getParticipant();
            var locale = retainedLocale(participant.getLocale());
            var reveal = revealService.view(locale);
            var entry = reveal.leaderboard().stream()
                    .filter(candidate -> candidate.participantId().equals(participant.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Participant has no effective result"));
            var leaderboardLink = UriComponentsBuilder.fromUri(publicUrl.baseUrl())
                    .pathSegment("results")
                    .queryParam("lang", locale.getLanguage())
                    .build()
                    .encode()
                    .toUri();
            notifier.send(participant, locale, composer.compose(reveal, entry, locale, leaderboardLink));
        } catch (RuntimeException exception) {
            recorder.markFailed(deliveryId, exception);
            return LinkDeliveryStatus.FAILED;
        }
        recorder.markSent(deliveryId);
        return LinkDeliveryStatus.SENT;
    }

    public LinkDeliveryStatus retry(UUID deliveryId) {
        var delivery = repository.findById(deliveryId).orElseThrow(NoRetryableRevealDeliveryException::new);
        if (delivery.getStatus() == LinkDeliveryStatus.SENT) {
            throw new NoRetryableRevealDeliveryException();
        }
        return deliver(deliveryId);
    }

    private Locale retainedLocale(String languageTag) {
        var locale = Locale.forLanguageTag(languageTag == null ? "" : languageTag);
        return locale.getLanguage().isBlank() ? Locale.ENGLISH : locale;
    }
}

package ch.babyguess.web.admin;

import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.event.PublicationOutcome;
import ch.babyguess.mail.LinkDeliveryStatus;
import ch.babyguess.mail.RevealEmailDelivery;
import ch.babyguess.mail.RevealEmailDeliveryRepository;
import ch.babyguess.participant.ParticipantRepository;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevealPublicationService {

    private final EventConfigurationService eventService;
    private final AdminRevealService revealService;
    private final ParticipantRepository participantRepository;
    private final RevealEmailDeliveryRepository deliveryRepository;
    private final Clock clock;

    public RevealPublicationService(
            EventConfigurationService eventService,
            AdminRevealService revealService,
            ParticipantRepository participantRepository,
            RevealEmailDeliveryRepository deliveryRepository,
            Clock clock) {
        this.eventService = eventService;
        this.revealService = revealService;
        this.participantRepository = participantRepository;
        this.deliveryRepository = deliveryRepository;
        this.clock = clock;
    }

    @Transactional
    public PublicationResult publish(long expectedVersion) {
        var outcome = eventService.publish(expectedVersion);
        if (outcome == PublicationOutcome.ALREADY_PUBLISHED) {
            return new PublicationResult(false, java.util.List.of());
        }

        var deliveryIds = new ArrayList<UUID>();
        for (var entry : revealService.view(Locale.ENGLISH).leaderboard()) {
            if (deliveryRepository.findByParticipantId(entry.participantId()).isPresent()) {
                continue;
            }
            var deliveryId = UUID.randomUUID();
            var participant = participantRepository.getReferenceById(entry.participantId());
            deliveryRepository.save(new RevealEmailDelivery(deliveryId, participant, clock.instant()));
            deliveryIds.add(deliveryId);
        }
        return new PublicationResult(true, java.util.List.copyOf(deliveryIds));
    }

    @Transactional(readOnly = true)
    public AdminRevealPublicationView status(Locale locale) {
        var event = eventService.get();
        var deliveries = deliveryRepository.findAllByOrderByCreatedAtAsc();
        var issues = deliveries.stream()
                .filter(delivery -> delivery.getStatus() != LinkDeliveryStatus.SENT)
                .map(delivery -> new AdminRevealPublicationView.DeliveryIssue(
                        delivery.getId(),
                        delivery.getParticipant().getDisplayName(),
                        delivery.getStatus(),
                        delivery.getAttemptCount(),
                        delivery.getFailureCode()))
                .toList();
        String publishedAt = null;
        if (event.getRevealedAt() != null) {
            var formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(locale)
                    .withZone(ZoneId.of(event.getEventTimezone()));
            publishedAt = formatter.format(event.getRevealedAt());
        }
        return new AdminRevealPublicationView(
                event.getVersion(),
                event.getRevealedAt() != null,
                publishedAt,
                deliveries.size(),
                Math.toIntExact(deliveries.stream()
                        .filter(delivery -> delivery.getStatus() == LinkDeliveryStatus.SENT).count()),
                Math.toIntExact(deliveries.stream()
                        .filter(delivery -> delivery.getStatus() == LinkDeliveryStatus.PENDING).count()),
                Math.toIntExact(deliveries.stream()
                        .filter(delivery -> delivery.getStatus() == LinkDeliveryStatus.FAILED).count()),
                issues);
    }
}

package ch.babyguess.web.admin;

import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.mail.LinkDeliveryStatus;
import ch.babyguess.mail.ParticipantLinkDelivery;
import ch.babyguess.mail.ParticipantLinkDeliveryRepository;
import ch.babyguess.participant.EditTokenService;
import ch.babyguess.participant.ParticipantRepository;
import ch.babyguess.submission.NameGuess;
import ch.babyguess.submission.SubmissionReceipt;
import ch.babyguess.submission.SubmissionVersion;
import ch.babyguess.submission.SubmissionVersionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminParticipantService {

    private final EventConfigurationService eventService;
    private final ParticipantRepository participantRepository;
    private final SubmissionVersionRepository submissionRepository;
    private final ParticipantLinkDeliveryRepository deliveryRepository;
    private final EditTokenService tokenService;

    public AdminParticipantService(
            EventConfigurationService eventService,
            ParticipantRepository participantRepository,
            SubmissionVersionRepository submissionRepository,
            ParticipantLinkDeliveryRepository deliveryRepository,
            EditTokenService tokenService) {
        this.eventService = eventService;
        this.participantRepository = participantRepository;
        this.submissionRepository = submissionRepository;
        this.deliveryRepository = deliveryRepository;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public AdminParticipantDashboard dashboard(Locale locale) {
        var event = eventService.get();
        var formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(ZoneId.of(event.getEventTimezone()));
        var submissions = groupSubmissions();
        var latestDeliveries = latestDeliveries();
        var entries = new ArrayList<AdminParticipantDashboard.ParticipantEntry>();
        long effectiveCount = 0;
        long failedCount = 0;

        for (var participant : participantRepository.findAllByOrderByCreatedAtAsc()) {
            var history = submissions.getOrDefault(participant.getId(), List.of());
            var effective = effectiveSubmission(history, event.getSubmissionDeadline());
            var historyEntries = new ArrayList<AdminParticipantDashboard.SubmissionEntry>();
            for (int index = 0; index < history.size(); index++) {
                var version = history.get(index);
                historyEntries.add(toEntry(
                        version,
                        history.size() - index,
                        effective != null && effective.getId().equals(version.getId()),
                        event.getSubmissionDeadline(),
                        formatter));
            }
            var effectiveEntry = effective == null
                    ? null
                    : toEntry(
                            effective,
                            history.size() - history.indexOf(effective),
                            true,
                            event.getSubmissionDeadline(),
                            formatter);
            var delivery = toEntry(latestDeliveries.get(participant.getId()), formatter);
            if (effectiveEntry != null) {
                effectiveCount++;
            }
            if (delivery != null && delivery.status() == LinkDeliveryStatus.FAILED) {
                failedCount++;
            }
            entries.add(new AdminParticipantDashboard.ParticipantEntry(
                    participant.getId(),
                    participant.getDisplayName(),
                    participant.getEmailAddress(),
                    formatter.format(participant.getCreatedAt()),
                    effectiveEntry,
                    List.copyOf(historyEntries),
                    delivery));
        }

        var deadline = event.getSubmissionDeadline() == null
                ? null
                : formatter.format(event.getSubmissionDeadline());
        return new AdminParticipantDashboard(
                deadline,
                entries.size(),
                effectiveCount,
                failedCount,
                List.copyOf(entries));
    }

    @Transactional(readOnly = true)
    public SubmissionReceipt prepareRetry(UUID participantId) {
        var participant = participantRepository.findById(participantId)
                .orElseThrow(NoRetryableLinkDeliveryException::new);
        var delivery = deliveryRepository.findFirstByParticipantIdOrderByCreatedAtDesc(participantId)
                .filter(candidate -> candidate.getStatus() == LinkDeliveryStatus.FAILED)
                .orElseThrow(NoRetryableLinkDeliveryException::new);
        var rawToken = tokenService.tokenFor(participantId);
        var derivedHash = tokenService.hash(rawToken);
        if (!MessageDigest.isEqual(
                derivedHash.getBytes(StandardCharsets.US_ASCII),
                participant.getEditTokenHash().getBytes(StandardCharsets.US_ASCII))) {
            throw new NoRetryableLinkDeliveryException(
                    "The configured token secret does not match this participant");
        }
        return new SubmissionReceipt(delivery.getId(), participant, rawToken);
    }

    private Map<UUID, List<SubmissionVersion>> groupSubmissions() {
        var grouped = new LinkedHashMap<UUID, List<SubmissionVersion>>();
        for (var submission : submissionRepository.findAllByOrderBySubmittedAtDesc()) {
            grouped.computeIfAbsent(submission.getParticipant().getId(), ignored -> new ArrayList<>())
                    .add(submission);
        }
        return grouped;
    }

    private Map<UUID, ParticipantLinkDelivery> latestDeliveries() {
        var latest = new LinkedHashMap<UUID, ParticipantLinkDelivery>();
        for (var delivery : deliveryRepository.findAllByOrderByCreatedAtDesc()) {
            latest.putIfAbsent(delivery.getParticipant().getId(), delivery);
        }
        return latest;
    }

    private SubmissionVersion effectiveSubmission(List<SubmissionVersion> history, Instant deadline) {
        if (deadline == null) {
            return null;
        }
        return history.stream()
                .filter(version -> !version.getSubmittedAt().isAfter(deadline))
                .findFirst()
                .orElse(null);
    }

    private AdminParticipantDashboard.SubmissionEntry toEntry(
            SubmissionVersion version,
            int versionNumber,
            boolean effective,
            Instant deadline,
            DateTimeFormatter formatter) {
        return new AdminParticipantDashboard.SubmissionEntry(
                versionNumber,
                formatter.format(version.getSubmittedAt()),
                effective,
                deadline == null || version.getSubmittedAt().isAfter(deadline),
                version.getNameGuesses().stream().map(NameGuess::getGuessedName).toList(),
                version.getPredictedSex(),
                version.getPredictedBirthDate(),
                version.getPredictedBirthWeightGrams());
    }

    private AdminParticipantDashboard.DeliveryEntry toEntry(
            ParticipantLinkDelivery delivery,
            DateTimeFormatter formatter) {
        if (delivery == null) {
            return null;
        }
        return new AdminParticipantDashboard.DeliveryEntry(
                delivery.getStatus(),
                delivery.getAttemptCount(),
                formatter.format(delivery.getCreatedAt()),
                delivery.getLastAttemptAt() == null ? null : formatter.format(delivery.getLastAttemptAt()),
                delivery.getFailureCode(),
                delivery.getStatus() == LinkDeliveryStatus.FAILED);
    }
}

package ch.babyguess.submission;

import ch.babyguess.event.EventConfiguration;
import ch.babyguess.config.SupportedLanguages;
import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.mail.ParticipantLinkDelivery;
import ch.babyguess.mail.ParticipantLinkDeliveryRepository;
import ch.babyguess.name.NameMatcher;
import ch.babyguess.participant.EditTokenService;
import ch.babyguess.participant.Participant;
import ch.babyguess.participant.ParticipantRepository;
import java.text.Normalizer;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipantSubmissionService {

    private final EventConfigurationService eventService;
    private final ParticipantRepository participantRepository;
    private final SubmissionVersionRepository submissionRepository;
    private final ParticipantLinkDeliveryRepository deliveryRepository;
    private final EditTokenService tokenService;
    private final NameMatcher nameMatcher;
    private final Clock clock;

    public ParticipantSubmissionService(
            EventConfigurationService eventService,
            ParticipantRepository participantRepository,
            SubmissionVersionRepository submissionRepository,
            ParticipantLinkDeliveryRepository deliveryRepository,
            EditTokenService tokenService,
            NameMatcher nameMatcher,
            Clock clock) {
        this.eventService = eventService;
        this.participantRepository = participantRepository;
        this.submissionRepository = submissionRepository;
        this.deliveryRepository = deliveryRepository;
        this.tokenService = tokenService;
        this.nameMatcher = nameMatcher;
        this.clock = clock;
    }

    @Transactional
    public SubmissionReceipt submit(ParticipantSubmission submission) {
        var event = eventService.get();
        var now = clock.instant();
        ensureOpen(event, now);

        var normalizedEmail = normalizeEmail(submission.emailAddress());
        var existing = participantRepository.findByNormalizedEmailAddress(normalizedEmail);
        if (existing.isPresent()) {
            return createDelivery(existing.get(), now);
        }

        var names = validateAndNormalizeNames(submission.nameGuesses(), event.getMaximumNameGuesses());
        var participantId = UUID.randomUUID();
        var rawToken = tokenService.tokenFor(participantId);
        var locale = supportedLocale(submission.locale());
        var participant = new Participant(
                participantId,
                requireText(submission.displayName(), 120, "Display name"),
                requireText(submission.emailAddress(), 320, "Email address"),
                normalizedEmail,
                tokenService.hash(rawToken),
                locale.toLanguageTag(),
                now);
        participantRepository.saveAndFlush(participant);

        var version = new SubmissionVersion(
                UUID.randomUUID(),
                participant,
                now,
                event.isSexEnabled() ? submission.predictedSex() : null,
                event.isBirthDateEnabled() ? submission.predictedBirthDate() : null,
                event.isBirthWeightEnabled() ? submission.predictedBirthWeightGrams() : null,
                names);
        submissionRepository.save(version);

        var delivery = new ParticipantLinkDelivery(UUID.randomUUID(), participant, now);
        deliveryRepository.save(delivery);
        return new SubmissionReceipt(delivery.getId(), participant, rawToken, false);
    }

    @Transactional(readOnly = true)
    public EditSubmissionView getForEdit(String rawToken) {
        var participant = findByToken(rawToken);
        var event = eventService.get();
        var effective = submissionRepository.findByParticipantIdOrderBySubmittedAtDesc(participant.getId()).stream()
                .filter(version -> event.getSubmissionDeadline() != null
                        && !version.getSubmittedAt().isAfter(event.getSubmissionDeadline()))
                .max(Comparator.comparing(SubmissionVersion::getSubmittedAt));
        if (effective.isEmpty()) {
            return new EditSubmissionView(
                    participant.getDisplayName(), participant.getEmailAddress(), List.of(), null, null, null);
        }
        var version = effective.get();
        var names = version.getNameGuesses().stream().map(NameGuess::getGuessedName).toList();
        return new EditSubmissionView(
                participant.getDisplayName(),
                participant.getEmailAddress(),
                names,
                version.getPredictedSex(),
                version.getPredictedBirthDate(),
                version.getPredictedBirthWeightGrams());
    }

    @Transactional
    public void edit(String rawToken, PredictionValues values) {
        var participant = findByToken(rawToken);
        var event = eventService.get();
        var now = clock.instant();
        ensureOpen(event, now);
        var names = validateAndNormalizeNames(values.nameGuesses(), event.getMaximumNameGuesses());
        var version = new SubmissionVersion(
                UUID.randomUUID(),
                participant,
                now,
                event.isSexEnabled() ? values.predictedSex() : null,
                event.isBirthDateEnabled() ? values.predictedBirthDate() : null,
                event.isBirthWeightEnabled() ? values.predictedBirthWeightGrams() : null,
                names);
        submissionRepository.save(version);
    }

    private SubmissionReceipt createDelivery(Participant participant, java.time.Instant now) {
        var delivery = new ParticipantLinkDelivery(UUID.randomUUID(), participant, now);
        deliveryRepository.save(delivery);
        return new SubmissionReceipt(
                delivery.getId(), participant, tokenService.tokenFor(participant.getId()), true);
    }

    private Participant findByToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) {
            throw new UnknownEditTokenException();
        }
        return participantRepository.findByEditTokenHash(tokenService.hash(rawToken))
                .orElseThrow(UnknownEditTokenException::new);
    }

    private void ensureOpen(EventConfiguration event, java.time.Instant now) {
        if (event.getSubmissionDeadline() == null || !now.isBefore(event.getSubmissionDeadline())) {
            throw new EventClosedException();
        }
    }

    private List<SubmittedName> validateAndNormalizeNames(List<String> guesses, int maximum) {
        var result = new ArrayList<SubmittedName>();
        var distinct = new HashSet<String>();
        boolean foundTrailingBlank = false;
        if (guesses != null) {
            for (String guess : guesses) {
                if (guess == null || guess.isBlank()) {
                    if (!result.isEmpty()) {
                        foundTrailingBlank = true;
                    }
                    continue;
                }
                if (foundTrailingBlank) {
                    throw new IllegalArgumentException("Name guesses cannot contain gaps");
                }
                var value = requireText(guess, 160, "Name guess");
                var cosmetic = nameMatcher.cosmeticForm(value);
                if (!distinct.add(cosmetic)) {
                    throw new IllegalArgumentException("Name guesses must be distinct");
                }
                result.add(new SubmittedName(value, cosmetic));
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("At least one name guess is required");
        }
        if (result.size() > maximum) {
            throw new IllegalArgumentException("Too many name guesses");
        }
        return List.copyOf(result);
    }

    private String normalizeEmail(String emailAddress) {
        return Normalizer.normalize(requireText(emailAddress, 320, "Email address"), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, int maximumLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        var stripped = value.strip();
        if (stripped.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " is too long");
        }
        return stripped;
    }

    private Locale supportedLocale(Locale locale) {
        return SupportedLanguages.resolve(locale);
    }
}

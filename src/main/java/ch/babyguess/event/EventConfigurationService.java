package ch.babyguess.event;

import ch.babyguess.scoring.ActualBaby;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventConfigurationService {

    private final EventConfigurationRepository repository;
    private final Clock clock;

    public EventConfigurationService(EventConfigurationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EventConfiguration get() {
        return repository.findById(EventConfiguration.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Event configuration row is missing"));
    }

    @Transactional
    public EventConfiguration update(EventConfigurationUpdate update) {
        var configuration = get();
        if (configuration.getVersion() != update.expectedVersion()) {
            throw new StaleEventConfigurationException();
        }
        configuration.update(update, clock.instant());
        return configuration;
    }

    @Transactional
    public EventConfiguration updateActualDetails(long expectedVersion, ActualBaby actual) {
        var configuration = get();
        if (configuration.getVersion() != expectedVersion) {
            throw new StaleEventConfigurationException();
        }
        var now = clock.instant();
        if (configuration.getSubmissionDeadline() == null
                || now.isBefore(configuration.getSubmissionDeadline())) {
            throw new RevealNotReadyException();
        }
        var retainedValues = new ActualBaby(
                actual.name(),
                configuration.isSexEnabled() ? actual.sex() : null,
                configuration.isBirthDateEnabled() ? actual.birthDate() : null,
                configuration.isBirthWeightEnabled() ? actual.birthWeightGrams() : null);
        configuration.updateActualDetails(retainedValues, now);
        return configuration;
    }

    @Transactional
    public PublicationOutcome publish(long expectedVersion) {
        var configuration = get();
        if (configuration.getRevealedAt() != null) {
            return PublicationOutcome.ALREADY_PUBLISHED;
        }
        if (configuration.getVersion() != expectedVersion) {
            throw new StaleEventConfigurationException();
        }
        var now = clock.instant();
        if (configuration.getSubmissionDeadline() == null
                || now.isBefore(configuration.getSubmissionDeadline())
                || configuration.getActualName() == null
                || configuration.getActualName().isBlank()) {
            throw new RevealNotReadyException();
        }
        configuration.markRevealed(now);
        return PublicationOutcome.FIRST_PUBLICATION;
    }

}

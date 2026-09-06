package ch.babyguess.mail;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipantLinkDeliveryRecorder {

    private final ParticipantLinkDeliveryRepository repository;
    private final Clock clock;

    public ParticipantLinkDeliveryRecorder(ParticipantLinkDeliveryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID deliveryId) {
        var delivery = repository.findById(deliveryId).orElseThrow();
        delivery.markSent(clock.instant());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID deliveryId, RuntimeException exception) {
        var delivery = repository.findById(deliveryId).orElseThrow();
        var failureCode = exception instanceof MailUnavailableException unavailable
                ? "MAIL_" + unavailable.getReason().name()
                : exception.getClass().getSimpleName();
        delivery.markFailed(clock.instant(), failureCode.substring(0, Math.min(failureCode.length(), 160)));
    }
}

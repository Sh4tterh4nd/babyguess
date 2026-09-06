package ch.babyguess.mail;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevealEmailDeliveryRecorder {

    private final RevealEmailDeliveryRepository repository;
    private final Clock clock;

    public RevealEmailDeliveryRecorder(RevealEmailDeliveryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID deliveryId) {
        var delivery = repository.findById(deliveryId).orElseThrow();
        if (delivery.getStatus() != LinkDeliveryStatus.SENT) {
            delivery.markSent(clock.instant());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID deliveryId, RuntimeException exception) {
        var delivery = repository.findById(deliveryId).orElseThrow();
        if (delivery.getStatus() == LinkDeliveryStatus.SENT) {
            return;
        }
        var failureCode = exception instanceof MailUnavailableException unavailable
                ? "MAIL_" + unavailable.getReason().name()
                : exception.getClass().getSimpleName();
        delivery.markFailed(clock.instant(), failureCode.substring(0, Math.min(failureCode.length(), 160)));
    }
}

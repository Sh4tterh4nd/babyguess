package ch.babyguess.mail;

import ch.babyguess.submission.SubmissionReceipt;
import java.net.URI;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ParticipantLinkDeliveryService {

    private final ParticipantLinkNotifier notifier;
    private final ParticipantLinkDeliveryRecorder recorder;

    public ParticipantLinkDeliveryService(
            ParticipantLinkNotifier notifier,
            ParticipantLinkDeliveryRecorder recorder) {
        this.notifier = notifier;
        this.recorder = recorder;
    }

    public LinkDeliveryStatus deliver(SubmissionReceipt receipt, URI publicBaseUri) {
        var editLink = UriComponentsBuilder.fromUri(publicBaseUri)
                .pathSegment("edit", receipt.rawToken())
                .build()
                .encode()
                .toUri();
        try {
            var retainedLocale = Locale.forLanguageTag(receipt.participant().getLocale());
            notifier.send(receipt.participant(), retainedLocale, editLink);
        } catch (RuntimeException exception) {
            recorder.markFailed(receipt.deliveryId(), exception);
            return LinkDeliveryStatus.FAILED;
        }
        recorder.markSent(receipt.deliveryId());
        return LinkDeliveryStatus.SENT;
    }
}

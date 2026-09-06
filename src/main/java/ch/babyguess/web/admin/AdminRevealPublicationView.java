package ch.babyguess.web.admin;

import ch.babyguess.mail.LinkDeliveryStatus;
import java.util.List;
import java.util.UUID;

public record AdminRevealPublicationView(
        long eventVersion,
        boolean published,
        String publishedAt,
        int deliveryTotal,
        int deliverySent,
        int deliveryPending,
        int deliveryFailed,
        List<DeliveryIssue> deliveryIssues) {

    public record DeliveryIssue(
            UUID deliveryId,
            String displayName,
            LinkDeliveryStatus status,
            int attemptCount,
            String failureCode) {

        public boolean isFailed() {
            return status == LinkDeliveryStatus.FAILED;
        }
    }
}

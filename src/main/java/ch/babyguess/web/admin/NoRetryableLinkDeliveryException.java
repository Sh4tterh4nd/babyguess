package ch.babyguess.web.admin;

public class NoRetryableLinkDeliveryException extends RuntimeException {

    public NoRetryableLinkDeliveryException() {
        super("No failed link delivery is available for retry");
    }

    public NoRetryableLinkDeliveryException(String message) {
        super(message);
    }
}

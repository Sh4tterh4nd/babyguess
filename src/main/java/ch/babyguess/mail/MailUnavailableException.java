package ch.babyguess.mail;

public class MailUnavailableException extends RuntimeException {

    public enum Reason {
        NOT_CONFIGURED,
        ENCRYPTION_KEY_UNAVAILABLE,
        INVALID_ENCRYPTED_PASSWORD
    }

    private final Reason reason;

    public MailUnavailableException() {
        this(Reason.NOT_CONFIGURED);
    }

    public MailUnavailableException(Reason reason) {
        super(safeMessage(reason));
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    private static String safeMessage(Reason reason) {
        return switch (reason) {
            case NOT_CONFIGURED -> "Outgoing email is not configured";
            case ENCRYPTION_KEY_UNAVAILABLE -> "The SMTP credential cannot be unlocked";
            case INVALID_ENCRYPTED_PASSWORD -> "The stored SMTP credential is invalid";
        };
    }
}

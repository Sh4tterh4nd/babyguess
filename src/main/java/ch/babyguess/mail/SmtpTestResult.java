package ch.babyguess.mail;

public enum SmtpTestResult {
    SENT,
    NOT_CONFIGURED,
    CREDENTIAL_UNAVAILABLE,
    AUTHENTICATION_FAILED,
    DELIVERY_FAILED
}

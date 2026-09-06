package ch.babyguess.mail;

public enum SmtpConfigurationProblem {
    STALE,
    HOST_REQUIRED,
    SENDER_REQUIRED,
    USERNAME_REQUIRED,
    PASSWORD_REQUIRED,
    ENCRYPTION_KEY_REQUIRED
}

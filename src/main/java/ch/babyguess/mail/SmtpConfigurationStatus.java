package ch.babyguess.mail;

public record SmtpConfigurationStatus(
        long version,
        boolean enabled,
        String host,
        int port,
        SmtpTransportSecurity transportSecurity,
        boolean authenticationRequired,
        String senderAddress,
        String username,
        boolean passwordStored,
        boolean encryptionAvailable,
        OutgoingMailConfigurationSource activeSource,
        boolean ready) {
}

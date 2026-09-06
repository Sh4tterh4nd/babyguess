package ch.babyguess.mail;

final class SmtpConnectionSettings {

    private final String host;
    private final int port;
    private final SmtpTransportSecurity transportSecurity;
    private final boolean authenticationRequired;
    private final String senderAddress;
    private final String username;
    private final String password;

    SmtpConnectionSettings(
            String host,
            int port,
            SmtpTransportSecurity transportSecurity,
            boolean authenticationRequired,
            String senderAddress,
            String username,
            String password) {
        this.host = host;
        this.port = port;
        this.transportSecurity = transportSecurity;
        this.authenticationRequired = authenticationRequired;
        this.senderAddress = senderAddress;
        this.username = username;
        this.password = password;
    }

    String host() { return host; }
    int port() { return port; }
    SmtpTransportSecurity transportSecurity() { return transportSecurity; }
    boolean authenticationRequired() { return authenticationRequired; }
    String senderAddress() { return senderAddress; }
    String username() { return username; }
    String password() { return password; }

    @Override
    public String toString() {
        return "SmtpConnectionSettings[host=" + host
                + ", port=" + port
                + ", transportSecurity=" + transportSecurity
                + ", authenticationRequired=" + authenticationRequired
                + ", senderAddress=" + senderAddress
                + ", username=[REDACTED]"
                + ", password=[REDACTED]]";
    }
}

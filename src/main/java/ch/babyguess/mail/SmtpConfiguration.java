package ch.babyguess.mail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "smtp_configuration")
public class SmtpConfiguration {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    @Version
    private long version;

    @Column(nullable = false)
    private boolean enabled;

    @Column(length = 255)
    private String host;

    @Column(nullable = false)
    private int port;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_security", nullable = false, length = 16)
    private SmtpTransportSecurity transportSecurity;

    @Column(name = "authentication_required", nullable = false)
    private boolean authenticationRequired;

    @Column(name = "sender_address", length = 320)
    private String senderAddress;

    @Column(length = 320)
    private String username;

    @Column(name = "encrypted_password", length = 4096)
    private String encryptedPassword;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SmtpConfiguration() {
    }

    public void update(
            boolean enabled,
            String host,
            int port,
            SmtpTransportSecurity transportSecurity,
            boolean authenticationRequired,
            String senderAddress,
            String username,
            String encryptedPassword,
            Instant now) {
        this.enabled = enabled;
        this.host = stripToNull(host);
        this.port = port;
        this.transportSecurity = transportSecurity;
        this.authenticationRequired = authenticationRequired;
        this.senderAddress = stripToNull(senderAddress);
        this.username = authenticationRequired ? stripToNull(username) : null;
        this.encryptedPassword = authenticationRequired ? encryptedPassword : null;
        this.updatedAt = now;
    }

    private String stripToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public Short getId() { return id; }
    public long getVersion() { return version; }
    public boolean isEnabled() { return enabled; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public SmtpTransportSecurity getTransportSecurity() { return transportSecurity; }
    public boolean isAuthenticationRequired() { return authenticationRequired; }
    public String getSenderAddress() { return senderAddress; }
    public String getUsername() { return username; }
    public String getEncryptedPassword() { return encryptedPassword; }
    public Instant getUpdatedAt() { return updatedAt; }
}

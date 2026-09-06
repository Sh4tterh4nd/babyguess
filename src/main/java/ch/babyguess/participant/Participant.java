package ch.babyguess.participant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "participant")
public class Participant {

    @Id
    private UUID id;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "email_address", nullable = false, length = 320)
    private String emailAddress;

    @Column(name = "normalized_email_address", nullable = false, unique = true, length = 320)
    private String normalizedEmailAddress;

    @Column(name = "edit_token_hash", nullable = false, unique = true, length = 64)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String editTokenHash;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Participant() {
    }

    public Participant(
            UUID id,
            String displayName,
            String emailAddress,
            String normalizedEmailAddress,
            String editTokenHash,
            String locale,
            Instant createdAt) {
        this.id = id;
        this.displayName = displayName;
        this.emailAddress = emailAddress;
        this.normalizedEmailAddress = normalizedEmailAddress;
        this.editTokenHash = editTokenHash;
        this.locale = locale;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getEmailAddress() { return emailAddress; }
    public String getNormalizedEmailAddress() { return normalizedEmailAddress; }
    public String getEditTokenHash() { return editTokenHash; }
    public String getLocale() { return locale; }
    public Instant getCreatedAt() { return createdAt; }
}

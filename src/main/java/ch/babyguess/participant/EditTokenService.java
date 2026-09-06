package ch.babyguess.participant;

import ch.babyguess.config.TokenProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class EditTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secret;

    public EditTokenService(TokenProperties properties) {
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
    }

    public String tokenFor(UUID participantId) {
        try {
            var mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(uuidBytes(participantId)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to create participant edit token", exception);
        }
    }

    public String hash(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to hash participant edit token", exception);
        }
    }

    private byte[] uuidBytes(UUID value) {
        return ByteBuffer.allocate(16)
                .putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits())
                .array();
    }
}

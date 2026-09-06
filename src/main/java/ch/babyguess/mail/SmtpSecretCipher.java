package ch.babyguess.mail;

import ch.babyguess.config.SecretEncryptionProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class SmtpSecretCipher {

    private static final String PREFIX = "v1:";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SmtpSecretCipher(SecretEncryptionProperties properties) {
        var configured = properties.encryptionKey();
        if (configured == null || configured.isBlank()) {
            key = null;
            return;
        }
        try {
            var decoded = Base64.getDecoder().decode(configured.strip());
            if (decoded.length != 32) {
                throw new IllegalArgumentException(
                        "BABYGUESS_SECRET_ENCRYPTION_KEY must decode to exactly 32 bytes");
            }
            key = new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "BABYGUESS_SECRET_ENCRYPTION_KEY must be a Base64-encoded 32-byte key", exception);
        }
    }

    public boolean isAvailable() {
        return key != null;
    }

    public String encrypt(String plaintext) {
        requireAvailable();
        try {
            var nonce = new byte[NONCE_LENGTH];
            random.nextBytes(nonce);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            var ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            var payload = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(ciphertext, 0, payload, nonce.length, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SMTP password encryption failed", exception);
        }
    }

    public String decrypt(String encrypted) {
        if (key == null) {
            throw new MailUnavailableException(MailUnavailableException.Reason.ENCRYPTION_KEY_UNAVAILABLE);
        }
        if (encrypted == null || !encrypted.startsWith(PREFIX)) {
            throw new MailUnavailableException(MailUnavailableException.Reason.INVALID_ENCRYPTED_PASSWORD);
        }
        try {
            var payload = Base64.getDecoder().decode(encrypted.substring(PREFIX.length()));
            if (payload.length <= NONCE_LENGTH) {
                throw new GeneralSecurityException("Invalid encrypted payload");
            }
            var nonce = java.util.Arrays.copyOfRange(payload, 0, NONCE_LENGTH);
            var ciphertext = java.util.Arrays.copyOfRange(payload, NONCE_LENGTH, payload.length);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new MailUnavailableException(MailUnavailableException.Reason.INVALID_ENCRYPTED_PASSWORD);
        }
    }

    private void requireAvailable() {
        if (key == null) {
            throw new MailUnavailableException(MailUnavailableException.Reason.ENCRYPTION_KEY_UNAVAILABLE);
        }
    }
}

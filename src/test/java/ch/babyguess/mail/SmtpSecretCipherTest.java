package ch.babyguess.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.babyguess.config.SecretEncryptionProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class SmtpSecretCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(
            "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Test
    void encryptsWithAUniqueNonceAndDecryptsWithoutExposingPlaintext() {
        var cipher = new SmtpSecretCipher(new SecretEncryptionProperties(KEY));

        var first = cipher.encrypt("smtp-secret");
        var second = cipher.encrypt("smtp-secret");

        assertThat(first).startsWith("v1:").doesNotContain("smtp-secret");
        assertThat(second).isNotEqualTo(first);
        assertThat(cipher.decrypt(first)).isEqualTo("smtp-secret");
    }

    @Test
    void refusesToStoreOrReadCredentialsWithoutTheDeploymentKey() {
        var cipher = new SmtpSecretCipher(new SecretEncryptionProperties(""));

        assertThat(cipher.isAvailable()).isFalse();
        assertThatThrownBy(() -> cipher.encrypt("smtp-secret"))
                .isInstanceOf(MailUnavailableException.class)
                .extracting("reason")
                .isEqualTo(MailUnavailableException.Reason.ENCRYPTION_KEY_UNAVAILABLE);
        assertThatThrownBy(() -> cipher.decrypt("v1:opaque"))
                .isInstanceOf(MailUnavailableException.class)
                .extracting("reason")
                .isEqualTo(MailUnavailableException.Reason.ENCRYPTION_KEY_UNAVAILABLE);
    }

    @Test
    void rejectsTamperedCiphertextWithASafeFailure() {
        var cipher = new SmtpSecretCipher(new SecretEncryptionProperties(KEY));

        assertThatThrownBy(() -> cipher.decrypt("v1:not-valid-ciphertext"))
                .isInstanceOf(MailUnavailableException.class)
                .hasMessageNotContaining("not-valid-ciphertext")
                .extracting("reason")
                .isEqualTo(MailUnavailableException.Reason.INVALID_ENCRYPTED_PASSWORD);
    }
}

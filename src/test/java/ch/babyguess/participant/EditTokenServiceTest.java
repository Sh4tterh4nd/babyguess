package ch.babyguess.participant;

import static org.assertj.core.api.Assertions.assertThat;

import ch.babyguess.config.TokenProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditTokenServiceTest {

    @Test
    void tokenIsStableUnguessableAndStoredOnlyAsAHash() {
        var service = new EditTokenService(new TokenProperties("a-test-secret-that-is-at-least-32-characters"));
        var participantId = UUID.fromString("a6e33f75-bb72-4c5c-97bf-b5d2da51b487");

        var token = service.tokenFor(participantId);

        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(service.tokenFor(participantId)).isEqualTo(token);
        assertThat(service.hash(token)).hasSize(64).doesNotContain(token);
    }

    @Test
    void changingSecretChangesTheToken() {
        var participantId = UUID.randomUUID();
        var first = new EditTokenService(new TokenProperties("first-test-secret-that-is-long-enough-123"));
        var second = new EditTokenService(new TokenProperties("second-test-secret-that-is-long-enough-12"));

        assertThat(first.tokenFor(participantId)).isNotEqualTo(second.tokenFor(participantId));
    }
}

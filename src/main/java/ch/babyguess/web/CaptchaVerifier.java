package ch.babyguess.web;

import ch.babyguess.config.CaptchaProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Verifies a Cap widget token against the configured instance.
 *
 * <p>Verification fails closed: a rejected token, an unexpected response, and an unreachable
 * instance are all refused, so taking the captcha service offline cannot bypass the check. The
 * secret key is sent only in the request body and never logged.
 */
@Service
public class CaptchaVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaptchaVerifier.class);
    private static final int MAXIMUM_TOKEN_LENGTH = 1024;

    private final CaptchaProperties properties;
    private final RestClient restClient;

    public CaptchaVerifier(CaptchaProperties properties, RestClient captchaRestClient) {
        this.properties = properties;
        this.restClient = captchaRestClient;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    /** The endpoint the browser widget posts its challenge to. */
    public String widgetEndpoint() {
        return properties.widgetEndpoint();
    }

    /**
     * @throws CaptchaVerificationException when the token is missing, rejected, or cannot be checked
     */
    public void verify(String token) {
        if (token == null || token.isBlank() || token.length() > MAXIMUM_TOKEN_LENGTH) {
            throw new CaptchaVerificationException(CaptchaFailure.REJECTED);
        }

        Map<String, Object> body;
        try {
            body = restClient.post()
                    .uri(properties.verificationEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("secret", properties.secretKey(), "response", token))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RuntimeException exception) {
            // The message can quote the request, so only the exception type is recorded.
            LOGGER.warn("Captcha verification could not be completed: {}", exception.getClass().getName());
            throw new CaptchaVerificationException(CaptchaFailure.UNAVAILABLE);
        }

        if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
            throw new CaptchaVerificationException(CaptchaFailure.REJECTED);
        }
    }

    public enum CaptchaFailure {
        /** The instance answered and refused the token. */
        REJECTED,
        /** The instance could not be reached or answered unusably. */
        UNAVAILABLE
    }

    public static class CaptchaVerificationException extends RuntimeException {

        private final CaptchaFailure failure;

        public CaptchaVerificationException(CaptchaFailure failure) {
            super("Captcha verification failed: " + failure);
            this.failure = failure;
        }

        public CaptchaFailure failure() {
            return failure;
        }
    }
}

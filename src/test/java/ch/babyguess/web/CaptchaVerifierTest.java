package ch.babyguess.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ch.babyguess.config.CaptchaProperties;
import ch.babyguess.web.CaptchaVerifier.CaptchaFailure;
import ch.babyguess.web.CaptchaVerifier.CaptchaVerificationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CaptchaVerifierTest {

    private static final String INSTANCE = "https://cap.example.test";
    private static final String SITE_KEY = "site-key-1";
    private static final String SECRET = "secret-key-1";

    @Test
    void configurationIsOnlyEnabledWhenEveryValueIsPresent() {
        assertThat(new CaptchaProperties(null, null, null).enabled()).isFalse();
        assertThat(new CaptchaProperties(INSTANCE, SITE_KEY, "  ").enabled()).isFalse();
        assertThat(new CaptchaProperties(INSTANCE, SITE_KEY, SECRET).enabled()).isTrue();
    }

    @Test
    void endpointsTolerateATrailingSlashOnTheInstanceUrl() {
        var properties = new CaptchaProperties(INSTANCE + "/", SITE_KEY, SECRET);

        assertThat(properties.widgetEndpoint()).isEqualTo(INSTANCE + "/" + SITE_KEY + "/");
        assertThat(properties.verificationEndpoint()).isEqualTo(INSTANCE + "/" + SITE_KEY + "/siteverify");
    }

    @Test
    void configurationRejectsAnInstanceUrlThatIsNotAbsoluteHttp() {
        assertThatThrownBy(() -> new CaptchaProperties("cap.example.test", SITE_KEY, SECRET))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theSecretKeyIsNeverExposedByToString() {
        assertThat(new CaptchaProperties(INSTANCE, SITE_KEY, SECRET).toString())
                .doesNotContain(SECRET)
                .contains("REDACTED");
    }

    @Test
    void anAcceptedTokenPassesVerification() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(INSTANCE + "/" + SITE_KEY + "/siteverify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"secret\":\"" + SECRET + "\",\"response\":\"token-1\"}"))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> verifier(builder).verify("token-1")).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void aRefusedTokenIsRejected() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(INSTANCE + "/" + SITE_KEY + "/siteverify"))
                .andRespond(withSuccess("{\"success\":false}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier(builder).verify("token-1"))
                .isInstanceOf(CaptchaVerificationException.class)
                .extracting(exception -> ((CaptchaVerificationException) exception).failure())
                .isEqualTo(CaptchaFailure.REJECTED);
    }

    @Test
    void aMissingTokenIsRejectedWithoutCallingTheInstance() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();

        assertThatThrownBy(() -> verifier(builder).verify(null))
                .isInstanceOf(CaptchaVerificationException.class)
                .extracting(exception -> ((CaptchaVerificationException) exception).failure())
                .isEqualTo(CaptchaFailure.REJECTED);
        server.verify();
    }

    @Test
    void anUnreachableInstanceFailsClosedAsUnavailable() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(INSTANCE + "/" + SITE_KEY + "/siteverify")).andRespond(withServerError());

        assertThatThrownBy(() -> verifier(builder).verify("token-1"))
                .isInstanceOf(CaptchaVerificationException.class)
                .extracting(exception -> ((CaptchaVerificationException) exception).failure())
                .isEqualTo(CaptchaFailure.UNAVAILABLE);
    }

    private CaptchaVerifier verifier(RestClient.Builder builder) {
        return new CaptchaVerifier(new CaptchaProperties(INSTANCE, SITE_KEY, SECRET), builder.build());
    }
}

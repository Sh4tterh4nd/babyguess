package ch.babyguess.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cap (trycap.dev) settings. The captcha is shown and enforced only when a deployment supplies all
 * three values; a deployment that leaves them blank keeps the previous submission behaviour.
 */
@ConfigurationProperties("babyguess.captcha")
public record CaptchaProperties(String instanceUrl, String siteKey, String secretKey) {

    public CaptchaProperties {
        instanceUrl = trimToNull(instanceUrl);
        siteKey = trimToNull(siteKey);
        secretKey = trimToNull(secretKey);
        if (instanceUrl != null) {
            var parsed = URI.create(instanceUrl);
            if (!parsed.isAbsolute() || parsed.getHost() == null
                    || !("http".equalsIgnoreCase(parsed.getScheme()) || "https".equalsIgnoreCase(parsed.getScheme()))) {
                throw new IllegalArgumentException("The captcha instance URL must be an absolute HTTP or HTTPS URL");
            }
        }
        if (siteKey != null && !siteKey.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("The captcha site key contains unsupported characters");
        }
    }

    /** True only when a deployment configured every value the widget and verification need. */
    public boolean enabled() {
        return instanceUrl != null && siteKey != null && secretKey != null;
    }

    /** The endpoint the browser widget talks to, always with a trailing slash. */
    public String widgetEndpoint() {
        return withoutTrailingSlash(instanceUrl) + "/" + siteKey + "/";
    }

    /** The server-side verification endpoint. */
    public String verificationEndpoint() {
        return withoutTrailingSlash(instanceUrl) + "/" + siteKey + "/siteverify";
    }

    @Override
    public String toString() {
        return "CaptchaProperties[instanceUrl=" + instanceUrl
                + ", siteKey=" + siteKey
                + ", secretKey=" + (secretKey == null ? "null" : "REDACTED") + "]";
    }

    private static String withoutTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        var stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}

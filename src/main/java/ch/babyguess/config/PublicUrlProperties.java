package ch.babyguess.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("babyguess.public")
public record PublicUrlProperties(URI baseUrl) {

    public PublicUrlProperties {
        if (baseUrl == null || !baseUrl.isAbsolute()) {
            throw new IllegalArgumentException("The public base URL must be absolute");
        }
        var scheme = baseUrl.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("The public base URL must use HTTP or HTTPS");
        }
        if (baseUrl.getHost() == null || baseUrl.getUserInfo() != null
                || baseUrl.getQuery() != null || baseUrl.getFragment() != null) {
            throw new IllegalArgumentException("The public base URL contains unsupported components");
        }
    }
}

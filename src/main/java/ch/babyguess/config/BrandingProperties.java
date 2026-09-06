package ch.babyguess.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("babyguess.branding")
public record BrandingProperties(Path directory) {

    public BrandingProperties {
        if (directory == null) {
            throw new IllegalArgumentException("The branding asset directory must be configured");
        }
    }
}

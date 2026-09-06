package ch.babyguess.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class ProductionConfigurationGuard {

    public ProductionConfigurationGuard(
            AdminProperties adminProperties,
            TokenProperties tokenProperties,
            PublicUrlProperties publicUrlProperties) {
        if ("admin12345678".equals(adminProperties.password())
                || "change-me-before-production".equals(adminProperties.password())) {
            throw new IllegalStateException("Set BABYGUESS_ADMIN_PASSWORD before starting the production profile");
        }
        if ("development-only-token-secret-change-me".equals(tokenProperties.secret())) {
            throw new IllegalStateException("Set BABYGUESS_TOKEN_SECRET before starting the production profile");
        }
        if ("localhost".equalsIgnoreCase(publicUrlProperties.baseUrl().getHost())) {
            throw new IllegalStateException("Set BABYGUESS_PUBLIC_BASE_URL before starting the production profile");
        }
    }
}

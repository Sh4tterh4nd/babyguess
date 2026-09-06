package ch.babyguess;

import ch.babyguess.config.AdminProperties;
import ch.babyguess.config.BrandingProperties;
import ch.babyguess.config.CaptchaProperties;
import ch.babyguess.config.TokenProperties;
import ch.babyguess.config.PublicUrlProperties;
import ch.babyguess.config.SecretEncryptionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        AdminProperties.class,
        BrandingProperties.class,
        CaptchaProperties.class,
        TokenProperties.class,
        PublicUrlProperties.class,
        SecretEncryptionProperties.class
})
public class BabyGuessApplication {

    public static void main(String[] args) {
        SpringApplication.run(BabyGuessApplication.class, args);
    }
}

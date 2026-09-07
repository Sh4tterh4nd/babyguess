package ch.babyguess.config;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Bean
    LocaleResolver localeResolver() {
        // A manual choice is stored in the cookie and wins. Without one, the browser's own
        // preference order decides, and anything unsupported falls back to English.
        var resolver = new CookieLocaleResolver("babyguess-language");
        resolver.setDefaultLocaleFunction(WebConfiguration::bestSupportedLocale);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }

    private static Locale bestSupportedLocale(HttpServletRequest request) {
        Enumeration<Locale> requested = request.getLocales();
        for (Locale candidate : Collections.list(requested)) {
            if (SupportedLanguages.isSupported(candidate)) {
                return SupportedLanguages.resolve(candidate);
            }
        }
        return SupportedLanguages.FALLBACK;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        var interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        registry.addInterceptor(interceptor);
    }
}

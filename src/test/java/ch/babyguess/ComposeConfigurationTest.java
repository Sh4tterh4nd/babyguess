package ch.babyguess;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class ComposeConfigurationTest {

    @Test
    void productionComposeUsesThePublishedImageAndPersistentH2Storage() throws IOException {
        var document = yaml("compose.production.yml");
        var services = map(document.get("services"));
        var application = map(services.get("babyguess"));
        var environment = map(application.get("environment"));

        assertThat(application.get("image"))
                .isEqualTo("shatterhand/babyguess:${BABYGUESS_IMAGE_TAG:-latest}");
        assertThat(list(application.get("ports")))
                .containsExactly("127.0.0.1:${BABYGUESS_HOST_PORT:-8080}:8080");
        assertThat(list(application.get("volumes"))).containsExactly("babyguess-data:/app/data");
        assertThat(environment.get("SPRING_PROFILES_ACTIVE")).isEqualTo("prod");
        assertThat(environment.get("BABYGUESS_DATABASE_URL").toString())
                .startsWith("jdbc:h2:file:/app/data/babyguess");
        assertThat(environment.get("BABYGUESS_PUBLIC_BASE_URL"))
                .isEqualTo("${BABYGUESS_PUBLIC_BASE_URL:?Set BABYGUESS_PUBLIC_BASE_URL}");
        assertThat(application.get("read_only")).isEqualTo(true);
        assertThat(services).containsKey("data-permissions");
    }

    @Test
    void developmentComposeCanBePromotedToStagingThroughEnvironmentValues() throws IOException {
        var document = yaml("compose.dev-staging.yml");
        var services = map(document.get("services"));
        var application = map(services.get("babyguess"));
        var environment = map(application.get("environment"));

        assertThat(application.get("image"))
                .isEqualTo("shatterhand/babyguess:${BABYGUESS_IMAGE_TAG:-dev}");
        assertThat(environment.get("SPRING_PROFILES_ACTIVE"))
                .isEqualTo("${SPRING_PROFILES_ACTIVE:-dev}");
        assertThat(environment.get("BABYGUESS_SECURE_COOKIES"))
                .isEqualTo("${BABYGUESS_SECURE_COOKIES:-false}");
        assertThat(list(application.get("volumes"))).containsExactly("babyguess-data:/app/data");
    }

    @Test
    void productionEnvironmentTemplateCannotLaunchWithPlaceholderSecrets() throws IOException {
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(".env.production.example"))) {
            properties.load(reader);
        }

        assertThat(properties.getProperty("BABYGUESS_DATABASE_PASSWORD")).isEmpty();
        assertThat(properties.getProperty("BABYGUESS_ADMIN_PASSWORD")).isEmpty();
        assertThat(properties.getProperty("BABYGUESS_TOKEN_SECRET")).isEmpty();
        assertThat(properties.getProperty("BABYGUESS_SECRET_ENCRYPTION_KEY")).isEmpty();
        assertThat(properties.getProperty("BABYGUESS_PUBLIC_BASE_URL")).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> yaml(String filename) throws IOException {
        try (var reader = Files.newBufferedReader(Path.of(filename))) {
            return new Yaml().load(reader);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value) {
        return (List<Object>) value;
    }
}

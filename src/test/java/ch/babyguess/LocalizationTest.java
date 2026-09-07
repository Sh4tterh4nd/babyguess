package ch.babyguess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.babyguess.config.SupportedLanguages;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:babyguess-localization;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class LocalizationTest {

    private static final List<String> BUNDLES =
            List.of("messages.properties", "messages_de.properties",
                    "messages_es.properties", "messages_pt.properties");

    @Autowired private MockMvc mockMvc;

    @Test
    void everyBundleDefinesExactlyTheSameKeys() throws IOException {
        var english = keys("messages.properties");

        assertThat(english).isNotEmpty();
        for (var bundle : BUNDLES) {
            assertThat(keys(bundle))
                    .as("keys in %s", bundle)
                    .containsExactlyInAnyOrderElementsOf(english);
        }
    }

    @Test
    void noTranslationIsLeftBlank() throws IOException {
        for (var bundle : BUNDLES) {
            for (var line : lines(bundle)) {
                assertThat(line.split("=", 2)[1].strip())
                        .as("value of %s in %s", line.split("=", 2)[0], bundle)
                        .isNotEmpty();
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
            "en, en", "de, de", "es, es", "pt, pt",
            "de-CH, de", "pt-BR, pt", "es-419, es", "en-GB, en",
            "fr, en", "ja, en"
    })
    void browserPreferencesChooseTheBestSupportedLanguage(String header, String expected) throws Exception {
        mockMvc.perform(get("/").header("Accept-Language", header))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"" + expected + "\"")));
    }

    @Test
    void aLowerPrioritySupportedLanguageWinsOverAnUnsupportedFirstChoice() throws Exception {
        mockMvc.perform(get("/").header("Accept-Language", "fr-FR,fr;q=0.9,pt;q=0.8"))
                .andExpect(content().string(containsString("<html lang=\"pt\"")));
    }

    @Test
    void anExplicitChoiceOverridesTheBrowserPreference() throws Exception {
        mockMvc.perform(get("/").param("lang", "es").header("Accept-Language", "de"))
                .andExpect(content().string(containsString("<html lang=\"es\"")));
    }

    @Test
    void theSelectorOffersEverySupportedLanguage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("value=\"en\"")))
                .andExpect(content().string(containsString("value=\"de\"")))
                .andExpect(content().string(containsString("value=\"es\"")))
                .andExpect(content().string(containsString("value=\"pt\"")));
    }

    @Test
    void regionalVariantsResolveToTheirBaseLanguage() {
        assertThat(SupportedLanguages.resolve(Locale.forLanguageTag("pt-BR")).getLanguage()).isEqualTo("pt");
        assertThat(SupportedLanguages.resolve(Locale.forLanguageTag("de-AT")).getLanguage()).isEqualTo("de");
        assertThat(SupportedLanguages.resolve(Locale.forLanguageTag("fr-CH"))).isEqualTo(Locale.ENGLISH);
        assertThat(SupportedLanguages.resolve(null)).isEqualTo(Locale.ENGLISH);
    }

    private List<String> lines(String bundle) throws IOException {
        return Files.readAllLines(Path.of("src/main/resources", bundle), StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .toList();
    }

    private LinkedHashSet<String> keys(String bundle) throws IOException {
        var keys = new LinkedHashSet<String>();
        for (var line : lines(bundle)) {
            keys.add(line.split("=", 2)[0]);
        }
        return keys;
    }
}

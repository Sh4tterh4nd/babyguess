package ch.babyguess.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ch.babyguess.event.EventConfiguration;
import ch.babyguess.event.EventConfigurationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:babyguess-admin-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@Transactional
class AdminEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventConfigurationRepository repository;

    @Test
    void redirectsAnonymousVisitorsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void rendersPersistedConfigurationForAdministrator() throws Exception {
        mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/index"))
                .andExpect(model().attributeExists("eventForm"));
    }

    @Test
    void requiresCsrfTokenWhenSaving() throws Exception {
        var current = configuration();

        mockMvc.perform(validPost(current.getVersion())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        assertThat(configuration().getTitle()).isEqualTo("BabyGuess");
    }

    @Test
    void savesValidatedSettingsAndConvertsDeadlineToInstant() throws Exception {
        var current = configuration();

        mockMvc.perform(validPost(current.getVersion())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("saved", true));

        var saved = configuration();
        assertThat(saved.getTitle()).isEqualTo("A little mystery");
        assertThat(saved.getSubmissionDeadline()).isEqualTo(Instant.parse("2026-10-01T10:30:00Z"));
        assertThat(saved.getEventTimezone()).isEqualTo("Europe/Zurich");
        assertThat(saved.getMaximumNameGuesses()).isEqualTo(4);
        assertThat(saved.isRankedNameScoring()).isTrue();
        assertThat(saved.isSexEnabled()).isTrue();
        assertThat(saved.isBirthDateEnabled()).isTrue();
        assertThat(saved.isBirthWeightEnabled()).isTrue();
        assertThat(saved.getBirthDateToleranceDays()).isEqualTo(7);
        assertThat(saved.getBirthWeightToleranceGrams()).isEqualTo(600);
        assertThat(saved.getNameWeight()).isEqualByComparingTo(new BigDecimal("2.5"));
    }

    @Test
    void rejectsInvalidSettingsWithoutChangingPersistence() throws Exception {
        var current = configuration();

        mockMvc.perform(post("/admin")
                        .param("version", Long.toString(current.getVersion()))
                        .param("title", " ")
                        .param("eventTimezone", "Not/A_Zone")
                        .param("maximumNameGuesses", "0")
                        .param("nameWeight", "-1")
                        .param("sexWeight", "1")
                        .param("birthDateWeight", "1")
                        .param("birthDateToleranceDays", "-1")
                        .param("birthWeightWeight", "1")
                        .param("birthWeightToleranceGrams", "-1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/index"))
                .andExpect(model().attributeHasFieldErrors(
                        "eventForm", "title", "submissionDeadline", "maximumNameGuesses",
                        "nameWeight", "birthDateToleranceDays", "birthWeightToleranceGrams"));

        assertThat(configuration().getTitle()).isEqualTo("BabyGuess");
    }

    @Test
    void refusesToOverwriteAConfigurationFromAnOlderForm() throws Exception {
        var current = configuration();

        mockMvc.perform(validPost(current.getVersion() + 1)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/index"))
                .andExpect(model().hasErrors());

        assertThat(configuration().getTitle()).isEqualTo("BabyGuess");
    }

    private MockHttpServletRequestBuilder validPost(long version) {
        return post("/admin")
                .param("version", Long.toString(version))
                .param("title", "  A little mystery  ")
                .param("submissionDeadline", "2026-10-01T12:30")
                .param("eventTimezone", "Europe/Zurich")
                .param("maximumNameGuesses", "4")
                .param("rankedNameScoring", "true")
                .param("nameWeight", "2.5")
                .param("sexEnabled", "true")
                .param("sexWeight", "1")
                .param("birthDateEnabled", "true")
                .param("birthDateWeight", "1.5")
                .param("birthDateToleranceDays", "7")
                .param("birthWeightEnabled", "true")
                .param("birthWeightWeight", "0.75")
                .param("birthWeightToleranceGrams", "600");
    }

    private EventConfiguration configuration() {
        return repository.findById(EventConfiguration.SINGLETON_ID).orElseThrow();
    }
}

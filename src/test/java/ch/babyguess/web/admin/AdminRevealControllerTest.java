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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.event.EventConfigurationUpdate;
import ch.babyguess.mail.ParticipantLinkDeliveryRepository;
import ch.babyguess.mail.LinkDeliveryStatus;
import ch.babyguess.mail.RevealEmailDeliveryRepository;
import ch.babyguess.mail.RevealResultNotifier;
import ch.babyguess.name.NameMatchDecisionRepository;
import ch.babyguess.name.NameMatchDecisionType;
import ch.babyguess.name.NameMatchType;
import ch.babyguess.participant.ParticipantRepository;
import ch.babyguess.scoring.Sex;
import ch.babyguess.submission.ParticipantSubmission;
import ch.babyguess.submission.ParticipantSubmissionService;
import ch.babyguess.submission.PredictionValues;
import ch.babyguess.submission.SubmissionVersionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:babyguess-admin-reveal;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class AdminRevealControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EventConfigurationService eventService;
    @Autowired private ParticipantSubmissionService submissionService;
    @Autowired private AdminRevealService revealService;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private SubmissionVersionRepository submissionRepository;
    @Autowired private ParticipantLinkDeliveryRepository deliveryRepository;
    @Autowired private RevealEmailDeliveryRepository revealDeliveryRepository;
    @Autowired private NameMatchDecisionRepository decisionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoBean private RevealResultNotifier revealResultNotifier;

    @BeforeEach
    void resetEvent() {
        decisionRepository.deleteAll();
        revealDeliveryRepository.deleteAll();
        deliveryRepository.deleteAll();
        submissionRepository.deleteAll();
        participantRepository.deleteAll();
        jdbcTemplate.update("""
                UPDATE event_configuration
                SET actual_name = NULL,
                    actual_sex = NULL,
                    actual_birth_date = NULL,
                    actual_birth_weight_grams = NULL,
                    revealed_at = NULL
                WHERE id = 1
                """);
        configureDeadline(Instant.now().plusSeconds(3600));
    }

    @Test
    void protectsRevealPreparationFromAnonymousVisitors() throws Exception {
        mockMvc.perform(get("/admin/reveal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void refusesActualDetailsWhilePredictionsAreOpen() throws Exception {
        var current = eventService.get();

        mockMvc.perform(actualDetailsPost(current.getVersion()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reveal"))
                .andExpect(model().hasErrors());

        assertThat(eventService.get().getActualName()).isNull();
    }

    @Test
    void savesActualDetailsAfterDeadlineAndRendersPrivatePreview() throws Exception {
        var email = UUID.randomUUID() + "@example.test";
        var receipt = submissionService.submit(submission("Exact Alice", email, "Sarah"));
        closeAtLatestSubmission();
        var current = eventService.get();

        mockMvc.perform(actualDetailsPost(current.getVersion()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reveal"))
                .andExpect(flash().attribute("detailsSaved", true));

        var result = mockMvc.perform(get("/admin/reveal").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reveal"))
                .andExpect(model().attributeExists("reveal", "actualDetailsForm", "variantForm"))
                .andReturn();
        var html = result.getResponse().getContentAsString();
        assertThat(html).contains("Exact Alice", "Sarah");
        assertThat(html).doesNotContain(email, receipt.rawToken());
        assertThat(eventService.get().getActualName()).isEqualTo("Sarah");
        assertThat(eventService.get().getActualSex()).isEqualTo(Sex.GIRL);
    }

    @Test
    void previewUsesEffectiveSnapshotAndDefinedNameTieBreakers() {
        var original = submissionService.submit(submission("Exact", randomEmail(), "Sarah"));
        submissionService.edit(original.rawToken(), new PredictionValues(List.of("Other"), null, null, null));
        var originalVersion = submissionRepository
                .findByParticipantIdOrderBySubmittedAtDesc(original.participant().getId())
                .getLast();
        submissionService.submit(submission("Equivalent", randomEmail(), "Sara"));
        submissionService.submit(submission("No match", randomEmail(), "Mara"));
        configureDeadline(originalVersion.getSubmittedAt());
        saveActual("Sarah");

        var preview = revealService.view(Locale.ENGLISH);

        assertThat(preview.leaderboard()).extracting(AdminRevealView.LeaderboardEntry::displayName)
                .containsExactly("Exact");
        assertThat(preview.leaderboard().getFirst().nameMatchType()).isEqualTo(NameMatchType.EXACT);
        assertThat(preview.eligibleParticipantCount()).isEqualTo(1);
    }

    @Test
    void acceptedCandidateIsRetainedAndRecalculatesNameScore() throws Exception {
        submissionService.submit(submission("Candidate", randomEmail(), "Saria"));
        closeAtLatestSubmission();
        saveActual("Sarah");
        assertThat(revealService.view(Locale.ENGLISH).candidates())
                .extracting(AdminRevealView.NameCandidate::displayName)
                .contains("Saria");

        mockMvc.perform(post("/admin/reveal/candidates")
                        .param("variant", "Saria")
                        .param("decision", "ACCEPTED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reveal#name-review"))
                .andExpect(flash().attribute("decisionSaved", true));

        var preview = revealService.view(Locale.ENGLISH);
        assertThat(preview.candidates()).isEmpty();
        assertThat(preview.decisions().getFirst().decision()).isEqualTo(NameMatchDecisionType.ACCEPTED);
        assertThat(preview.leaderboard().getFirst().nameMatchType()).isEqualTo(NameMatchType.EQUIVALENT);
        mockMvc.perform(get("/admin/reveal").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reveal"));
    }

    @Test
    void rejectedCandidateCanBeChangedLater() throws Exception {
        submissionService.submit(submission("Candidate", randomEmail(), "Saria"));
        closeAtLatestSubmission();
        saveActual("Sarah");
        decide("/admin/reveal/candidates", "Saria", "REJECTED");

        assertThat(revealService.view(Locale.ENGLISH).leaderboard().getFirst().nameMatchType())
                .isEqualTo(NameMatchType.NONE);

        decide("/admin/reveal/decisions", "Saria", "ACCEPTED");

        assertThat(decisionRepository.findAll().getFirst().getDecision())
                .isEqualTo(NameMatchDecisionType.ACCEPTED);
        assertThat(revealService.view(Locale.ENGLISH).leaderboard().getFirst().nameMatchType())
                .isEqualTo(NameMatchType.EQUIVALENT);
    }

    @Test
    void manualVariantSupportsAValidSpellingThatWasNotSuggested() throws Exception {
        submissionService.submit(submission("Manual", randomEmail(), "Sarina"));
        closeAtLatestSubmission();
        saveActual("Sarah");
        assertThat(revealService.view(Locale.ENGLISH).candidates()).isEmpty();

        mockMvc.perform(post("/admin/reveal/variants")
                        .param("variant", "Sarina")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reveal#name-review"));

        assertThat(revealService.view(Locale.ENGLISH).leaderboard().getFirst().nameMatchType())
                .isEqualTo(NameMatchType.EQUIVALENT);
    }

    @Test
    void revealWritesRequireCsrfToken() throws Exception {
        closeAtLatestSubmission();
        var current = eventService.get();

        mockMvc.perform(actualDetailsPost(current.getVersion()))
                .andExpect(status().isForbidden());

        assertThat(eventService.get().getActualName()).isNull();
    }

    @Test
    void publicationIsExplicitIdempotentAndOpensOnlyTheSanitizedLeaderboard() throws Exception {
        var email = randomEmail();
        var receipt = submissionService.submit(submission("Exact Alice", email, "Sarah"));
        closeAtLatestSubmission();
        saveActual("Sarah");
        var versionAtPublication = eventService.get().getVersion();

        mockMvc.perform(post("/admin/reveal/publish")
                        .param("version", Long.toString(versionAtPublication))
                        .param("confirmed", "true")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reveal#publish-reveal"))
                .andExpect(flash().attribute("published", true));

        assertThat(eventService.get().getRevealedAt()).isNotNull();
        assertThat(revealDeliveryRepository.count()).isEqualTo(1);
        var delivery = revealDeliveryRepository.findAll().getFirst();
        assertThat(delivery.getStatus()).isEqualTo(LinkDeliveryStatus.SENT);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);

        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/results"));
        var publicResult = mockMvc.perform(get("/results"))
                .andExpect(status().isOk())
                .andExpect(view().name("reveal"))
                .andExpect(model().attributeExists("reveal"))
                .andReturn();
        assertThat(publicResult.getResponse().getContentAsString())
                .contains("Sarah", "Exact Alice", "Leaderboard")
                .doesNotContain(email, receipt.rawToken());

        var correctedEvent = eventService.get();
        eventService.updateActualDetails(
                correctedEvent.getVersion(),
                new ch.babyguess.scoring.ActualBaby("Mila", null, null, null));
        var correctedPublicResult = mockMvc.perform(get("/results"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(correctedPublicResult.getResponse().getContentAsString())
                .contains("Mila")
                .doesNotContain("Sarah");

        mockMvc.perform(post("/admin/reveal/publish")
                        .param("version", Long.toString(versionAtPublication))
                        .param("confirmed", "true")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("alreadyPublished", true));

        assertThat(revealDeliveryRepository.count()).isEqualTo(1);
        assertThat(revealDeliveryRepository.findAll().getFirst().getAttemptCount()).isEqualTo(1);
        verify(revealResultNotifier, times(1))
                .send(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publicationRequiresConfirmationAndCsrf() throws Exception {
        closeAtLatestSubmission();
        saveActual("Sarah");
        var current = eventService.get();

        mockMvc.perform(post("/admin/reveal/publish")
                        .param("version", Long.toString(current.getVersion()))
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("publicationConfirmationRequired", true));
        assertThat(eventService.get().getRevealedAt()).isNull();

        mockMvc.perform(post("/admin/reveal/publish")
                        .param("version", Long.toString(current.getVersion()))
                        .param("confirmed", "true")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        assertThat(eventService.get().getRevealedAt()).isNull();
    }

    private void decide(String path, String variant, String decision) throws Exception {
        mockMvc.perform(post(path)
                        .param("variant", variant)
                        .param("decision", decision)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("decisionSaved", true));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder actualDetailsPost(long version) {
        return post("/admin/reveal/details")
                .param("version", Long.toString(version))
                .param("actualName", "  Sarah  ")
                .param("actualSex", "GIRL")
                .param("actualBirthDate", "2026-10-10")
                .param("actualBirthWeightGrams", "3500")
                .with(user("admin").roles("ADMIN"));
    }

    private ParticipantSubmission submission(String displayName, String email, String name) {
        return new ParticipantSubmission(displayName, email, List.of(name), null, null, null, Locale.ENGLISH);
    }

    private String randomEmail() {
        return UUID.randomUUID() + "@example.test";
    }

    private void closeAtLatestSubmission() {
        var deadline = submissionRepository.findAllByOrderBySubmittedAtDesc().stream()
                .map(version -> version.getSubmittedAt())
                .max(Instant::compareTo)
                .orElseGet(() -> Instant.now().minusSeconds(1));
        configureDeadline(deadline);
    }

    private void saveActual(String name) {
        var event = eventService.get();
        eventService.updateActualDetails(
                event.getVersion(),
                new ch.babyguess.scoring.ActualBaby(name, null, null, null));
    }

    private void configureDeadline(Instant deadline) {
        var current = eventService.get();
        eventService.update(new EventConfigurationUpdate(
                current.getVersion(),
                "A little mystery",
                deadline,
                "Europe/Zurich",
                3,
                false,
                BigDecimal.ONE,
                true,
                BigDecimal.ONE,
                true,
                BigDecimal.ONE,
                5,
                true,
                BigDecimal.ONE,
                500));
    }
}

package ch.babyguess.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.event.EventConfigurationUpdate;
import ch.babyguess.mail.LinkDeliveryStatus;
import ch.babyguess.mail.ParticipantLinkDeliveryRepository;
import ch.babyguess.participant.ParticipantRepository;
import ch.babyguess.scoring.Sex;
import ch.babyguess.submission.ParticipantSubmission;
import ch.babyguess.submission.ParticipantSubmissionService;
import ch.babyguess.submission.PredictionValues;
import ch.babyguess.submission.SubmissionVersionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:babyguess-participant-flow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class ParticipantFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EventConfigurationService eventService;
    @Autowired private ParticipantSubmissionService submissionService;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private SubmissionVersionRepository submissionRepository;
    @Autowired private ParticipantLinkDeliveryRepository deliveryRepository;

    @BeforeEach
    void openEvent() {
        configureDeadline(Instant.now().plusSeconds(3600));
    }

    @Test
    void publicPageReflectsEnabledCategories() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("eventOpen", true))
                .andExpect(model().attribute("sexEnabled", true))
                .andExpect(model().attribute("birthDateEnabled", true))
                .andExpect(model().attribute("birthWeightEnabled", true));
    }

    @Test
    void initialSubmissionCreatesParticipantSnapshotAndTrackedMailAttempt() throws Exception {
        long participantsBefore = participantRepository.count();
        long submissionsBefore = submissionRepository.count();
        long deliveriesBefore = deliveryRepository.count();

        mockMvc.perform(validSubmission(randomEmail()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/thanks"));

        assertThat(participantRepository.count()).isEqualTo(participantsBefore + 1);
        assertThat(submissionRepository.count()).isEqualTo(submissionsBefore + 1);
        assertThat(deliveryRepository.count()).isEqualTo(deliveriesBefore + 1);
        assertThat(deliveryRepository.findAll().get((int) deliveriesBefore).getStatus())
                .isEqualTo(LinkDeliveryStatus.FAILED);
    }

    @Test
    void duplicateEmailDoesNotCreateAnotherParticipantOrSnapshot() {
        var email = randomEmail();
        var first = submissionService.submit(submission(email, List.of("Sara")));
        long participants = participantRepository.count();
        long versions = submissionRepository.count();

        var second = submissionService.submit(submission(email.toUpperCase(Locale.ROOT), List.of("Different")));

        assertThat(participantRepository.count()).isEqualTo(participants);
        assertThat(submissionRepository.count()).isEqualTo(versions);
        assertThat(second.rawToken()).isEqualTo(first.rawToken());
        assertThat(second.toString()).doesNotContain(second.rawToken()).contains("REDACTED");
    }

    @Test
    void duplicateNormalizedNamesAreRejectedWithoutSaving() throws Exception {
        long before = submissionRepository.count();

        mockMvc.perform(post("/submit")
                        .param("displayName", "Alice")
                        .param("emailAddress", randomEmail())
                        .param("nameGuesses[0]", "Sarah")
                        .param("nameGuesses[1]", "  SARAH ")
                        .param("predictedSex", "GIRL")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeHasFieldErrors("submissionForm", "nameGuesses"));

        assertThat(submissionRepository.count()).isEqualTo(before);
    }

    @Test
    void serverRejectsSubmissionAtOrAfterDeadline() throws Exception {
        configureDeadline(Instant.now().minusSeconds(1));
        long before = submissionRepository.count();

        mockMvc.perform(validSubmission(randomEmail()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().hasErrors());

        assertThat(submissionRepository.count()).isEqualTo(before);
    }

    @Test
    void validEditTokenLoadsSubmissionAndAppendsVersion() throws Exception {
        var receipt = submissionService.submit(submission(randomEmail(), List.of("Sara", "Noah")));
        long before = submissionRepository.countByParticipantId(receipt.participant().getId());

        mockMvc.perform(get("/edit/{token}", receipt.rawToken()))
                .andExpect(status().isOk())
                .andExpect(view().name("edit"))
                .andExpect(model().attribute("displayName", "Alice"));

        mockMvc.perform(post("/edit/{token}", receipt.rawToken())
                        .param("nameGuesses[0]", "Rebecca")
                        .param("nameGuesses[1]", "Mara")
                        .param("predictedSex", "BOY")
                        .param("predictedBirthDate", "2026-11-12")
                        .param("predictedBirthWeightGrams", "3600")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/edit/" + receipt.rawToken() + "?saved"));

        assertThat(submissionRepository.countByParticipantId(receipt.participant().getId())).isEqualTo(before + 1);
        var current = submissionService.getForEdit(receipt.rawToken());
        assertThat(current.nameGuesses()).containsExactly("Rebecca", "Mara");
        assertThat(current.predictedSex()).isEqualTo(Sex.BOY);

        var history = submissionRepository.findByParticipantIdOrderBySubmittedAtDesc(receipt.participant().getId());
        var original = history.get(history.size() - 1);
        configureDeadline(original.getSubmittedAt());

        var effectiveAfterMovingDeadlineBack = submissionService.getForEdit(receipt.rawToken());
        assertThat(effectiveAfterMovingDeadlineBack.nameGuesses()).containsExactly("Sara", "Noah");
    }

    @Test
    void unknownEditTokenReturnsNotFound() throws Exception {
        mockMvc.perform(get("/edit/not-a-real-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicWritesRequireCsrfToken() throws Exception {
        mockMvc.perform(validSubmission(randomEmail()))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validSubmission(String email) {
        return post("/submit")
                .param("displayName", "Alice")
                .param("emailAddress", email)
                .param("nameGuesses[0]", "Sara")
                .param("nameGuesses[1]", "Noah")
                .param("predictedSex", "GIRL")
                .param("predictedBirthDate", "2026-11-10")
                .param("predictedBirthWeightGrams", "3500");
    }

    private ParticipantSubmission submission(String email, List<String> names) {
        return new ParticipantSubmission(
                "Alice", email, names, Sex.GIRL, LocalDate.of(2026, 11, 10), 3500, Locale.ENGLISH);
    }

    private String randomEmail() {
        return UUID.randomUUID() + "@example.test";
    }

    private void configureDeadline(Instant deadline) {
        var current = eventService.get();
        eventService.update(new EventConfigurationUpdate(
                current.getVersion(),
                "A little mystery",
                deadline,
                "Europe/Zurich",
                3,
                true,
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

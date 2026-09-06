package ch.babyguess.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ch.babyguess.config.PublicUrlProperties;
import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.event.EventConfigurationUpdate;
import ch.babyguess.mail.LinkDeliveryStatus;
import ch.babyguess.mail.ParticipantLinkDeliveryRepository;
import ch.babyguess.mail.ParticipantLinkDeliveryService;
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
        "spring.datasource.url=jdbc:h2:mem:babyguess-admin-participants;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class AdminParticipantControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EventConfigurationService eventService;
    @Autowired private ParticipantSubmissionService submissionService;
    @Autowired private SubmissionVersionRepository submissionRepository;
    @Autowired private ParticipantLinkDeliveryRepository deliveryRepository;
    @Autowired private ParticipantLinkDeliveryService deliveryService;
    @Autowired private PublicUrlProperties publicUrlProperties;
    @Autowired private AdminParticipantService adminParticipantService;

    @BeforeEach
    void openEvent() {
        configureDeadline(Instant.now().plusSeconds(3600));
    }

    @Test
    void protectsParticipantLedgerFromAnonymousVisitors() throws Exception {
        mockMvc.perform(get("/admin/participants"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void rendersEffectiveSubmissionAndCompleteVersionHistory() throws Exception {
        var displayName = "Ledger " + UUID.randomUUID();
        var receipt = submissionService.submit(submission(displayName));
        submissionService.edit(receipt.rawToken(), new PredictionValues(
                List.of("Rebecca", "Mara"), Sex.BOY, LocalDate.of(2026, 11, 12), 3600));

        var result = mockMvc.perform(get("/admin/participants").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/participants"))
                .andExpect(model().attributeExists("dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(displayName)))
                .andReturn();

        var dashboard = (AdminParticipantDashboard) result.getModelAndView().getModel().get("dashboard");
        assertThat(result.getResponse().getContentAsString()).doesNotContain(receipt.rawToken());
        var participant = dashboard.participants().stream()
                .filter(entry -> entry.id().equals(receipt.participant().getId()))
                .findFirst()
                .orElseThrow();
        assertThat(participant.history()).hasSize(2);
        assertThat(participant.effectiveSubmission().nameGuesses()).containsExactly("Rebecca", "Mara");
        assertThat(participant.effectiveSubmission().effective()).isTrue();
    }

    @Test
    void dashboardUsesEarlierVersionWhenDeadlineMovesBack() {
        var receipt = submissionService.submit(submission("Deadline " + UUID.randomUUID()));
        var original = submissionRepository.findByParticipantIdOrderBySubmittedAtDesc(receipt.participant().getId())
                .getFirst();
        submissionService.edit(receipt.rawToken(), new PredictionValues(
                List.of("Later"), Sex.BOY, LocalDate.of(2026, 11, 12), 3600));
        configureDeadline(original.getSubmittedAt());

        var participant = adminParticipantService.dashboard(Locale.ENGLISH).participants().stream()
                .filter(entry -> entry.id().equals(receipt.participant().getId()))
                .findFirst()
                .orElseThrow();

        assertThat(participant.effectiveSubmission().nameGuesses()).containsExactly("Sara", "Noah");
        assertThat(participant.history()).filteredOn(AdminParticipantDashboard.SubmissionEntry::afterDeadline)
                .hasSize(1);
    }

    @Test
    void retryUsesSameFailedDeliveryAndIncrementsAttempts() throws Exception {
        var displayName = "Retry " + UUID.randomUUID();
        var receipt = submissionService.submit(submission(displayName));
        assertThat(deliveryService.deliver(receipt, publicUrlProperties.baseUrl()))
                .isEqualTo(LinkDeliveryStatus.FAILED);
        long deliveriesBefore = deliveryRepository.count();

        mockMvc.perform(post("/admin/participants/{participantId}/retry-link", receipt.participant().getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/participants"))
                .andExpect(flash().attribute("deliveryFailed", displayName));

        var delivery = deliveryRepository.findById(receipt.deliveryId()).orElseThrow();
        assertThat(deliveryRepository.count()).isEqualTo(deliveriesBefore);
        assertThat(delivery.getStatus()).isEqualTo(LinkDeliveryStatus.FAILED);
        assertThat(delivery.getAttemptCount()).isEqualTo(2);
        assertThat(delivery.getFailureCode()).isEqualTo("MAIL_NOT_CONFIGURED");
    }

    @Test
    void retryRequiresCsrfToken() throws Exception {
        var receipt = submissionService.submit(submission("CSRF " + UUID.randomUUID()));
        deliveryService.deliver(receipt, publicUrlProperties.baseUrl());

        mockMvc.perform(post("/admin/participants/{participantId}/retry-link", receipt.participant().getId())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        assertThat(deliveryRepository.findById(receipt.deliveryId()).orElseThrow().getAttemptCount()).isEqualTo(1);
    }

    @Test
    void refusesRetryWhenLatestDeliveryHasNotFailed() throws Exception {
        var receipt = submissionService.submit(submission("Pending " + UUID.randomUUID()));

        mockMvc.perform(post("/admin/participants/{participantId}/retry-link", receipt.participant().getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/participants"))
                .andExpect(flash().attribute("deliveryUnavailable", true));

        assertThat(deliveryRepository.findById(receipt.deliveryId()).orElseThrow().getStatus())
                .isEqualTo(LinkDeliveryStatus.PENDING);
    }

    private ParticipantSubmission submission(String displayName) {
        return new ParticipantSubmission(
                displayName,
                UUID.randomUUID() + "@example.test",
                List.of("Sara", "Noah"),
                Sex.GIRL,
                LocalDate.of(2026, 11, 10),
                3500,
                Locale.ENGLISH);
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

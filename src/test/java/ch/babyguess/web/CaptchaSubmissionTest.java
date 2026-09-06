package ch.babyguess.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.event.EventConfigurationUpdate;
import ch.babyguess.participant.ParticipantRepository;
import ch.babyguess.web.CaptchaVerifier.CaptchaFailure;
import ch.babyguess.web.CaptchaVerifier.CaptchaVerificationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The captcha is configured through the environment, so these tests supply the properties and stub
 * the verifier rather than reaching a real Cap instance.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:babyguess-captcha;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "babyguess.captcha.instance-url=https://cap.example.test",
        "babyguess.captcha.site-key=site-key-1",
        "babyguess.captcha.secret-key=secret-key-1"
})
@AutoConfigureMockMvc
class CaptchaSubmissionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EventConfigurationService eventService;
    @Autowired private ParticipantRepository participantRepository;
    @MockitoBean private CaptchaVerifier captchaVerifier;

    @BeforeEach
    void openEventWithCaptchaConfigured() {
        var current = eventService.get();
        eventService.update(new EventConfigurationUpdate(
                current.getVersion(),
                "A little mystery",
                Instant.now().plusSeconds(3600),
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
        given(captchaVerifier.enabled()).willReturn(true);
        given(captchaVerifier.widgetEndpoint()).willReturn("https://cap.example.test/site-key-1/");
    }

    @Test
    void theWidgetIsRenderedOnTheSubmissionFormWhenConfigured() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<cap-widget")))
                .andExpect(content().string(containsString("https://cap.example.test/site-key-1/")))
                .andExpect(content().string(containsString("cap-widget@0.1.57")));
    }

    @Test
    void theWidgetIsAbsentWhenTheCaptchaIsNotConfigured() throws Exception {
        given(captchaVerifier.enabled()).willReturn(false);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<cap-widget"))))
                .andExpect(content().string(not(containsString("cap-widget@"))));
    }

    @Test
    void anAcceptedTokenLetsTheSubmissionThrough() throws Exception {
        willDoNothing().given(captchaVerifier).verify("token-1");
        long before = participantRepository.count();

        mockMvc.perform(validSubmission().param("cap-token", "token-1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/thanks"));

        assertThat(participantRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void aRejectedTokenKeepsTheSubmissionOutOfTheDatabase() throws Exception {
        willThrow(new CaptchaVerificationException(CaptchaFailure.REJECTED))
                .given(captchaVerifier).verify(any());
        long before = participantRepository.count();

        mockMvc.perform(validSubmission().param("cap-token", "wrong").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));

        assertThat(participantRepository.count()).isEqualTo(before);
    }

    @Test
    void anUnreachableInstanceKeepsTheSubmissionOutOfTheDatabase() throws Exception {
        willThrow(new CaptchaVerificationException(CaptchaFailure.UNAVAILABLE))
                .given(captchaVerifier).verify(any());
        long before = participantRepository.count();

        mockMvc.perform(validSubmission().with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("Verification is unavailable")));

        assertThat(participantRepository.count()).isEqualTo(before);
    }

    private MockHttpServletRequestBuilder validSubmission() {
        return post("/submit")
                .param("displayName", "Alice")
                .param("emailAddress", UUID.randomUUID() + "@example.test")
                .param("nameGuesses[0]", "Sara")
                .param("predictedSex", "GIRL");
    }
}

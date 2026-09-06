package ch.babyguess.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ch.babyguess.mail.SmtpConfiguration;
import ch.babyguess.mail.SmtpConfigurationRepository;
import ch.babyguess.mail.SmtpTestResult;
import ch.babyguess.mail.SmtpTestService;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:babyguess-admin-email;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "babyguess.secrets.encryption-key=" + AdminEmailControllerTest.KEY
})
@AutoConfigureMockMvc
@Transactional
class AdminEmailControllerTest {

    static final String KEY = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
    private static final String PASSWORD = "visible-only-on-submit";

    @Autowired private MockMvc mockMvc;
    @Autowired private SmtpConfigurationRepository repository;
    @MockitoBean private SmtpTestService testService;

    @Test
    void redirectsAnonymousVisitorsToLogin() throws Exception {
        mockMvc.perform(get("/admin/email"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void rendersTheProtectedWriteOnlyConfigurationPage() throws Exception {
        var response = mockMvc.perform(get("/admin/email").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/email"))
                .andExpect(model().attributeExists("smtpForm", "smtpStatus", "testEmailForm"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).contains("replacementPassword").doesNotContain(PASSWORD);
    }

    @Test
    void requiresCsrfTokenWhenSaving() throws Exception {
        mockMvc.perform(configurationPost(0, PASSWORD)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        assertThat(configuration().isEnabled()).isFalse();
    }

    @Test
    void encryptsThePasswordAndNeverReturnsItToTheBrowser() throws Exception {
        mockMvc.perform(configurationPost(configuration().getVersion(), PASSWORD)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/email"))
                .andExpect(flash().attribute("smtpSaved", true));

        repository.flush();
        var saved = configuration();
        assertThat(saved.getEncryptedPassword())
                .startsWith("v1:")
                .doesNotContain(PASSWORD);

        var response = mockMvc.perform(get("/admin/email").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain(PASSWORD).doesNotContain(saved.getEncryptedPassword());
    }

    @Test
    void aBlankPasswordKeepsTheEncryptedCredential() throws Exception {
        mockMvc.perform(configurationPost(configuration().getVersion(), PASSWORD)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        repository.flush();
        var first = configuration().getEncryptedPassword();
        var version = configuration().getVersion();

        mockMvc.perform(configurationPost(version, "")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        repository.flush();

        assertThat(configuration().getEncryptedPassword()).isEqualTo(first);
    }

    @Test
    void reportsTheSanitizedTestResult() throws Exception {
        when(testService.send("host@example.test", Locale.ENGLISH))
                .thenReturn(SmtpTestResult.AUTHENTICATION_FAILED);

        mockMvc.perform(post("/admin/email/test")
                        .param("recipient", "host@example.test")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .locale(Locale.ENGLISH))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/email"))
                .andExpect(flash().attribute("emailTestResult", "AUTHENTICATION_FAILED"));
    }

    @Test
    void neverEchoesAnInvalidSubmittedPassword() throws Exception {
        var oversizedSecret = "s".repeat(1025);

        var response = mockMvc.perform(configurationPost(configuration().getVersion(), oversizedSecret)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/email"))
                .andExpect(model().attributeHasFieldErrors("smtpForm", "replacementPassword"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain(oversizedSecret);
        assertThat(configuration().getEncryptedPassword()).isNull();
    }

    private MockHttpServletRequestBuilder configurationPost(long version, String password) {
        return post("/admin/email")
                .param("version", Long.toString(version))
                .param("enabled", "true")
                .param("host", "smtp.example.test")
                .param("port", "587")
                .param("transportSecurity", "STARTTLS")
                .param("authenticationRequired", "true")
                .param("senderAddress", "hello@example.test")
                .param("username", "smtp-user")
                .param("replacementPassword", password);
    }

    private SmtpConfiguration configuration() {
        return repository.findById(SmtpConfiguration.SINGLETON_ID).orElseThrow();
    }
}

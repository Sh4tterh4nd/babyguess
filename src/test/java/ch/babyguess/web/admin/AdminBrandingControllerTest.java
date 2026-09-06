package ch.babyguess.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ch.babyguess.branding.BrandingAssetContent;
import ch.babyguess.branding.BrandingAssetStorage;
import ch.babyguess.event.EventConfiguration;
import ch.babyguess.event.EventConfigurationRepository;
import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.event.EventConfigurationUpdate;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:babyguess-admin-branding;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@Transactional
class AdminBrandingControllerTest {

    private static final String ASSET_NAME = "00000000-0000-0000-0000-000000000001.png";

    @Autowired private MockMvc mockMvc;
    @Autowired private EventConfigurationRepository repository;
    @Autowired private EventConfigurationService eventService;
    @MockitoBean private BrandingAssetStorage assetStorage;

    @Test
    void protectsBrandingAdministrationFromAnonymousVisitors() throws Exception {
        mockMvc.perform(get("/admin/branding"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void rendersBothFixedBrandingSlots() throws Exception {
        mockMvc.perform(get("/admin/branding").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/branding"))
                .andExpect(model().attributeExists("branding"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("image/png,image/jpeg")));
    }

    @Test
    void requiresCsrfBeforeAcceptingAnUpload() throws Exception {
        mockMvc.perform(multipart("/admin/branding/logo")
                        .file(upload())
                        .param("version", Long.toString(configuration().getVersion()))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        assertThat(configuration().getLogoAssetName()).isNull();
    }

    @Test
    void activatesAnUploadedLogoAndServesItOnlyThroughTheFixedEndpoint() throws Exception {
        when(assetStorage.store(any())).thenReturn(ASSET_NAME);
        when(assetStorage.load(ASSET_NAME)).thenReturn(new BrandingAssetContent(
                new ByteArrayResource("safe-image".getBytes(StandardCharsets.UTF_8)),
                MediaType.IMAGE_PNG));

        mockMvc.perform(multipart("/admin/branding/logo")
                        .file(upload())
                        .param("version", Long.toString(configuration().getVersion()))
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/branding"))
                .andExpect(flash().attribute("brandingSaved", "LOGO"));

        assertThat(configuration().getLogoAssetName()).isEqualTo(ASSET_NAME);
        mockMvc.perform(get("/branding/logo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes("safe-image".getBytes(StandardCharsets.UTF_8)));
        mockMvc.perform(get("/branding/" + ASSET_NAME))
                .andExpect(status().isNotFound());
    }

    @Test
    void everyWordmarkUsesTheConfiguredPublicTitle() throws Exception {
        renameEvent("Welcome, little one");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<span class=\"wordmark-title\">Welcome, little one</span>")));
        mockMvc.perform(get("/admin/branding").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<span class=\"wordmark-title\">Welcome, little one</span>")));
    }

    @Test
    void refusesAStaleBrandingForm() throws Exception {
        when(assetStorage.store(any())).thenReturn(ASSET_NAME);

        mockMvc.perform(multipart("/admin/branding/background")
                        .file(upload())
                        .param("version", Long.toString(configuration().getVersion() + 1))
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("brandingError", "STALE"));

        assertThat(configuration().getBackgroundAssetName()).isNull();
    }

    private MockMultipartFile upload() {
        return new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, new byte[] {1, 2, 3});
    }

    private void renameEvent(String title) {
        var current = configuration();
        eventService.update(new EventConfigurationUpdate(
                current.getVersion(),
                title,
                current.getSubmissionDeadline(),
                current.getEventTimezone(),
                current.getMaximumNameGuesses(),
                current.isRankedNameScoring(),
                current.getNameWeight(),
                current.isSexEnabled(),
                current.getSexWeight(),
                current.isBirthDateEnabled(),
                current.getBirthDateWeight(),
                current.getBirthDateToleranceDays(),
                current.isBirthWeightEnabled(),
                current.getBirthWeightWeight(),
                current.getBirthWeightToleranceGrams()));
        repository.flush();
    }

    private EventConfiguration configuration() {
        return repository.findById(EventConfiguration.SINGLETON_ID).orElseThrow();
    }
}

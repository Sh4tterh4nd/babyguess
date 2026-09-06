package ch.babyguess.branding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.babyguess.config.BrandingProperties;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class BrandingAssetStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void validatesAndStoresDecodedImagesUnderGeneratedNames() throws Exception {
        var storage = new BrandingAssetStorage(new BrandingProperties(temporaryDirectory));
        var image = new MockMultipartFile(
                "file",
                "../../untrusted-name.png",
                MediaType.IMAGE_PNG_VALUE,
                png(80, 40));

        var storedName = storage.store(image);
        var loaded = storage.load(storedName);

        assertThat(storedName)
                .matches("[0-9a-f-]{36}\\.png")
                .doesNotContain("untrusted-name");
        assertThat(loaded.mediaType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(loaded.resource().getInputStream().readAllBytes()).isEqualTo(image.getBytes());
        try (var files = Files.list(temporaryDirectory)) {
            assertThat(files.toList()).hasSize(1);
        }
    }

    @Test
    void rejectsFilesThatOnlyClaimToBeImages() {
        var storage = new BrandingAssetStorage(new BrandingProperties(temporaryDirectory));
        var executable = new MockMultipartFile(
                "file", "logo.png", MediaType.IMAGE_PNG_VALUE, "MZ-not-an-image".getBytes());

        assertThatThrownBy(() -> storage.store(executable))
                .isInstanceOf(BrandingUploadException.class)
                .extracting("problem")
                .isEqualTo(BrandingUploadProblem.UNSUPPORTED_TYPE);
        assertThat(temporaryDirectory).isEmptyDirectory();
    }

    @Test
    void rejectsOversizedAndUnsafeDimensionImages() throws Exception {
        var storage = new BrandingAssetStorage(new BrandingProperties(temporaryDirectory));
        var oversized = new MockMultipartFile(
                "file", "huge.png", MediaType.IMAGE_PNG_VALUE,
                new byte[(int) BrandingAssetStorage.MAX_FILE_SIZE_BYTES + 1]);

        assertThatThrownBy(() -> storage.store(oversized))
                .isInstanceOf(BrandingUploadException.class)
                .extracting("problem")
                .isEqualTo(BrandingUploadProblem.TOO_LARGE);

        var tooWide = new MockMultipartFile(
                "file", "wide.png", MediaType.IMAGE_PNG_VALUE, png(8_001, 1));
        assertThatThrownBy(() -> storage.store(tooWide))
                .isInstanceOf(BrandingUploadException.class)
                .extracting("problem")
                .isEqualTo(BrandingUploadProblem.INVALID_DIMENSIONS);
        assertThat(temporaryDirectory).isEmptyDirectory();
    }

    @Test
    void neverResolvesUntrustedAssetNames() {
        var storage = new BrandingAssetStorage(new BrandingProperties(temporaryDirectory));

        assertThatThrownBy(() -> storage.load("../application.yml"))
                .isInstanceOf(BrandingAssetNotFoundException.class);
        assertThatThrownBy(() -> storage.load("logo.png"))
                .isInstanceOf(BrandingAssetNotFoundException.class);
    }

    private byte[] png(int width, int height) throws Exception {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(119, 117, 215));
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}

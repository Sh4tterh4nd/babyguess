package ch.babyguess.branding;

import ch.babyguess.config.BrandingProperties;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class BrandingAssetStorage {

    static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    static final int MAX_WIDTH = 8_000;
    static final int MAX_HEIGHT = 8_000;
    static final long MAX_PIXELS = 40_000_000L;

    private static final Pattern SAFE_NAME = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg)");

    private final Path storageDirectory;

    public BrandingAssetStorage(BrandingProperties properties) {
        storageDirectory = properties.directory().toAbsolutePath().normalize();
    }

    public String store(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw rejected(BrandingUploadProblem.EMPTY);
        }
        if (upload.getSize() > MAX_FILE_SIZE_BYTES) {
            throw rejected(BrandingUploadProblem.TOO_LARGE);
        }

        Path temporary = null;
        try {
            Files.createDirectories(storageDirectory);
            temporary = Files.createTempFile(storageDirectory, "incoming-", ".upload");
            copyBounded(upload.getInputStream(), temporary);
            var extension = validateImage(temporary);
            var assetName = UUID.randomUUID() + "." + extension;
            var target = resolveSafe(assetName);
            moveAtomically(temporary, target);
            temporary = null;
            return assetName;
        } catch (BrandingUploadException exception) {
            throw exception;
        } catch (IOException exception) {
            throw rejected(BrandingUploadProblem.STORAGE_UNAVAILABLE);
        } finally {
            deletePathQuietly(temporary);
        }
    }

    public BrandingAssetContent load(String assetName) {
        final Path path;
        try {
            path = resolveSafe(assetName);
        } catch (BrandingAssetNotFoundException exception) {
            throw exception;
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new BrandingAssetNotFoundException();
        }
        var mediaType = assetName.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return new BrandingAssetContent(new FileSystemResource(path), mediaType);
    }

    public void delete(String assetName) {
        if (assetName == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(assetName));
        } catch (IOException | BrandingAssetNotFoundException ignored) {
            // The generated name is safe; an orphan is preferable to breaking an active request.
        }
    }

    private void copyBounded(InputStream input, Path target) throws IOException {
        try (input; OutputStream output = Files.newOutputStream(
                target, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            var buffer = new byte[16 * 1024];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_FILE_SIZE_BYTES) {
                    throw rejected(BrandingUploadProblem.TOO_LARGE);
                }
                output.write(buffer, 0, read);
            }
        }
    }

    private String validateImage(Path imagePath) {
        try (var imageInput = ImageIO.createImageInputStream(imagePath.toFile())) {
            if (imageInput == null) {
                throw rejected(BrandingUploadProblem.CORRUPT_IMAGE);
            }
            var readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw rejected(BrandingUploadProblem.UNSUPPORTED_TYPE);
            }
            var reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                var format = reader.getFormatName().toLowerCase(Locale.ROOT);
                var extension = switch (format) {
                    case "png" -> "png";
                    case "jpeg", "jpg" -> "jpg";
                    default -> throw rejected(BrandingUploadProblem.UNSUPPORTED_TYPE);
                };
                var width = reader.getWidth(0);
                var height = reader.getHeight(0);
                if (width < 1 || height < 1
                        || width > MAX_WIDTH || height > MAX_HEIGHT
                        || (long) width * height > MAX_PIXELS) {
                    throw rejected(BrandingUploadProblem.INVALID_DIMENSIONS);
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw rejected(BrandingUploadProblem.CORRUPT_IMAGE);
                }
                return extension;
            } finally {
                reader.dispose();
            }
        } catch (BrandingUploadException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw rejected(BrandingUploadProblem.CORRUPT_IMAGE);
        }
    }

    private Path resolveSafe(String assetName) {
        if (assetName == null || !SAFE_NAME.matcher(assetName).matches()) {
            throw new BrandingAssetNotFoundException();
        }
        var resolved = storageDirectory.resolve(assetName).normalize();
        if (!resolved.getParent().equals(storageDirectory)) {
            throw new BrandingAssetNotFoundException();
        }
        return resolved;
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private void deletePathQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best effort cleanup of a rejected temporary file.
        }
    }

    private BrandingUploadException rejected(BrandingUploadProblem problem) {
        return new BrandingUploadException(problem);
    }
}

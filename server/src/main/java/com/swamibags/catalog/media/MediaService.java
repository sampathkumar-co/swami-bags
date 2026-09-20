package com.swamibags.catalog.media;

import com.swamibags.catalog.config.AppProperties;
import com.swamibags.catalog.product.ProductImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_PIXELS = 25_000_000L;
    private static final int MAX_DIMENSION = 10_000;

    private final Path dataDir;
    private final Path mediaRoot;

    public MediaService(AppProperties properties) throws IOException {
        this.dataDir = properties.dataDir().toAbsolutePath().normalize();
        this.mediaRoot = dataDir.resolve("media").normalize();
        Files.createDirectories(mediaRoot);
        SharedFilePermissions.makeDirectoryPublicReadable(mediaRoot);
    }

    public StoredMedia saveOriginal(String productId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Select at least one product image.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Each product image must be 10 MB or smaller.");
        }

        String declaredType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(declaredType)) {
            throw new IllegalArgumentException("Only JPEG and PNG product images are supported.");
        }

        DecodedImage decoded = decodeAndValidate(file, declaredType);

        Path directory = productDirectory(productId).resolve("originals");
        Files.createDirectories(directory);
        SharedFilePermissions.makeDirectoryPublicReadable(directory.getParent());
        SharedFilePermissions.makeDirectoryPublicReadable(directory);
        Path target = safeResolve(directory, UUID.randomUUID() + decoded.extension());

        boolean written = ImageIO.write(decoded.image(), decoded.format(), target.toFile());
        if (!written) {
            Files.deleteIfExists(target);
            throw new IllegalArgumentException("The uploaded image format could not be safely stored.");
        }
        SharedFilePermissions.makeFilePublicReadable(target);
        return stored(target);
    }

    public StoredMedia saveMarketing(String productId, byte[] jpegBytes) throws IOException {
        Path directory = productDirectory(productId).resolve("marketing");
        Files.createDirectories(directory);
        SharedFilePermissions.makeDirectoryPublicReadable(directory.getParent());
        SharedFilePermissions.makeDirectoryPublicReadable(directory);
        Path target = safeResolve(directory, UUID.randomUUID() + ".jpg");
        Files.write(target, jpegBytes);
        SharedFilePermissions.makeFilePublicReadable(target);
        return stored(target);
    }

    public Path resolve(ProductImage image) {
        Path resolved = dataDir.resolve(image.path()).normalize();
        if (!resolved.startsWith(mediaRoot)) {
            throw new IllegalArgumentException("Invalid media path.");
        }
        return resolved;
    }

    public void delete(ProductImage image) {
        try {
            Files.deleteIfExists(resolve(image));
        } catch (IOException ignored) {
            // Database deletion should not be blocked by an already-missing file.
        }
    }

    public void deleteProductDirectory(String productId) {
        Path directory = productDirectory(productId);
        if (!Files.exists(directory)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best effort cleanup.
                }
            });
        } catch (IOException ignored) {
            // Best effort cleanup.
        }
    }

    private DecodedImage decodeAndValidate(MultipartFile file, String declaredType) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) {
                throw new IllegalArgumentException("The uploaded file is not a readable image.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("The uploaded file does not match its declared image format.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String actualFormat = normalizeFormat(reader.getFormatName());
                String expectedFormat = "image/png".equals(declaredType) ? "png" : "jpeg";
                if (!expectedFormat.equals(actualFormat)) {
                    throw new IllegalArgumentException("The uploaded image content does not match its declared image format.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * height;
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION || pixels > MAX_PIXELS) {
                    throw new IllegalArgumentException("Product images must be at most 10,000 px per side and 25 megapixels.");
                }

                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IllegalArgumentException("The uploaded image could not be decoded.");
                }
                return new DecodedImage(image, actualFormat, "png".equals(actualFormat) ? ".png" : ".jpg");
            } finally {
                reader.dispose();
            }
        }
    }

    private String normalizeFormat(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "jpg", "jpeg" -> "jpeg";
            case "png" -> "png";
            default -> normalized;
        };
    }

    private Path productDirectory(String productId) {
        String safeId = productId.replaceAll("[^A-Za-z0-9._-]", "_");
        return mediaRoot.resolve("products").resolve(safeId).normalize();
    }

    private Path safeResolve(Path directory, String filename) {
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(directory.normalize())) {
            throw new IllegalArgumentException("Invalid media filename.");
        }
        return target;
    }

    private StoredMedia stored(Path target) {
        Path relative = dataDir.relativize(target);
        String relativeText = relative.toString().replace('\\', '/');
        return new StoredMedia(relativeText, "/" + relativeText);
    }

    private record DecodedImage(BufferedImage image, String format, String extension) {}

    public record StoredMedia(String relativePath, String publicUrl) {}
}

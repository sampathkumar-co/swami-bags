package com.swamibags.catalog.media;

import com.swamibags.catalog.config.AppProperties;
import com.swamibags.catalog.product.ProductImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

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
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG and WebP product images are supported.");
        }

        String extension = extensionFor(contentType);
        Path directory = productDirectory(productId).resolve("originals");
        Files.createDirectories(directory);
        SharedFilePermissions.makeDirectoryPublicReadable(directory.getParent());
        SharedFilePermissions.makeDirectoryPublicReadable(directory);
        String filename = UUID.randomUUID() + extension;
        Path target = safeResolve(directory, filename);

        try (var input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
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

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    public record StoredMedia(String relativePath, String publicUrl) {}
}

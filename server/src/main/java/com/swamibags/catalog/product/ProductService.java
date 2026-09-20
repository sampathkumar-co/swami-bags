package com.swamibags.catalog.product;

import com.swamibags.catalog.media.MediaService;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final MediaService media;
    private final CatalogExporter exporter;

    public ProductService(ProductRepository repository, MediaService media, CatalogExporter exporter) {
        this.repository = repository;
        this.media = media;
        this.exporter = exporter;
    }

    public List<Product> all() {
        return repository.findAll();
    }

    public Product get(String id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found."));
    }

    @Transactional
    public Product create(ProductRequest request) {
        String id = normalizeId(request.id());
        String slug = uniqueSlug(request.slug(), request.name(), id);
        String now = Instant.now().toString();

        var product = new Product(
                id,
                slug,
                clean(request.name()),
                clean(request.category()),
                clean(request.material()),
                request.price() == null ? BigDecimal.ZERO : request.price(),
                blankTo(request.priceUnit(), "piece"),
                request.moq() == null ? 1 : request.moq(),
                request.stock() == null ? 0 : request.stock(),
                request.restockDays(),
                blankTo(request.size(), ""),
                blankTo(request.description(), ""),
                sanitizeFeatures(request.features()),
                false,
                List.of(),
                now,
                now);

        try {
            repository.create(product);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("A product with that code or slug already exists.");
        }
        exporter.export();
        return get(id);
    }

    @Transactional
    public Product update(String id, ProductRequest request) {
        Product current = get(id);
        String slug = slugify(request.slug() == null || request.slug().isBlank() ? request.name() : request.slug());
        var updated = new Product(
                current.id(),
                slug,
                clean(request.name()),
                clean(request.category()),
                clean(request.material()),
                request.price() == null ? BigDecimal.ZERO : request.price(),
                blankTo(request.priceUnit(), "piece"),
                request.moq() == null ? 1 : request.moq(),
                request.stock() == null ? 0 : request.stock(),
                request.restockDays(),
                blankTo(request.size(), ""),
                blankTo(request.description(), ""),
                sanitizeFeatures(request.features()),
                current.published(),
                current.images(),
                current.createdAt(),
                Instant.now().toString());
        try {
            repository.update(updated);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Another product already uses that slug.");
        }
        exporter.export();
        return get(id);
    }

    @Transactional
    public Product setPublished(String id, boolean published) {
        Product product = get(id);
        if (published && product.images().stream().noneMatch(ProductImage::isOriginal)) {
            throw new IllegalArgumentException("Upload at least one real product image before publishing.");
        }
        repository.setPublished(id, published);
        exporter.export();
        return get(id);
    }

    @Transactional
    public List<ProductImage> addOriginalImages(String id, List<MultipartFile> files) throws IOException {
        get(id);
        if (files == null || files.isEmpty() || files.size() > 6) {
            throw new IllegalArgumentException("Upload between 1 and 6 product images.");
        }

        List<ProductImage> existingImages = repository.findImages(id);
        long existingOriginals = existingImages.stream().filter(ProductImage::isOriginal).count();
        if (existingOriginals + files.size() > 6) {
            throw new IllegalArgumentException("A product can have at most 6 original reference photos.");
        }

        int sort = existingImages.stream().filter(ProductImage::isOriginal)
                .mapToInt(ProductImage::sortOrder).max().orElse(-1) + 1;

        for (MultipartFile file : files) {
            var stored = media.saveOriginal(id, file);
            repository.addImage(id, "ORIGINAL", stored.relativePath(), stored.publicUrl(), sort++, false);
        }
        exporter.export();
        return get(id).images();
    }

    @Transactional
    public void removeImage(String id, String imageId) {
        get(id);
        ProductImage image = repository.findImage(id, imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found."));
        media.delete(image);
        repository.deleteImage(id, imageId);
        exporter.export();
    }

    @Transactional
    public Product approveMarketingImage(String id, String imageId) {
        get(id);
        repository.approveMarketingImage(id, imageId);
        exporter.export();
        return get(id);
    }

    @Transactional
    public void delete(String id) {
        get(id);
        repository.delete(id);
        media.deleteProductDirectory(id);
        exporter.export();
    }

    public Dashboard dashboard(boolean aiConfigured) {
        return new Dashboard(
                repository.countAll(),
                repository.countPublished(),
                repository.countOutOfStock(),
                repository.countImages(),
                aiConfigured);
    }

    private String uniqueSlug(String requested, String name, String id) {
        String base = slugify(requested == null || requested.isBlank() ? name : requested);
        if (base.isBlank()) {
            base = slugify(id);
        }
        String candidate = base;
        int suffix = 2;
        while (slugExists(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean slugExists(String candidate) {
        return repository.findAll().stream().anyMatch(product -> product.slug().equals(candidate));
    }

    private String normalizeId(String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "-");
        }
        return "SB-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String slugify(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required product field is missing.");
        }
        return value.trim();
    }

    private String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private List<String> sanitizeFeatures(List<String> features) {
        if (features == null) {
            return List.of();
        }
        return features.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .limit(8)
                .toList();
    }

    public record Dashboard(int products, int published, int outOfStock, int images, boolean aiConfigured) {}
}

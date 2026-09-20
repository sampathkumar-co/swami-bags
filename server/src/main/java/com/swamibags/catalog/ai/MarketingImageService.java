package com.swamibags.catalog.ai;

import com.swamibags.catalog.config.AppProperties;
import com.swamibags.catalog.media.MediaService;
import com.swamibags.catalog.product.CatalogExporter;
import com.swamibags.catalog.product.Product;
import com.swamibags.catalog.product.ProductImage;
import com.swamibags.catalog.product.ProductRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingImageService {
    private final ProductRepository repository;
    private final MediaService media;
    private final OpenAiImageService openAi;
    private final MarketingPosterService poster;
    private final CatalogExporter exporter;
    private final JdbcTemplate jdbc;
    private final AppProperties properties;

    public MarketingImageService(
            ProductRepository repository,
            MediaService media,
            OpenAiImageService openAi,
            MarketingPosterService poster,
            CatalogExporter exporter,
            JdbcTemplate jdbc,
            AppProperties properties) {
        this.repository = repository;
        this.media = media;
        this.openAi = openAi;
        this.poster = poster;
        this.exporter = exporter;
        this.jdbc = jdbc;
        this.properties = properties;
    }

    @Transactional
    public ProductImage generate(String productId) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        List<ProductImage> originals = product.images().stream().filter(ProductImage::isOriginal).limit(4).toList();
        if (originals.isEmpty()) {
            throw new IllegalArgumentException("Upload at least one real product photo before generating a marketing image.");
        }

        String prompt = buildPrompt(product);
        String generationId = UUID.randomUUID().toString();
        String createdAt = Instant.now().toString();
        jdbc.update("""
                INSERT INTO ai_generations (id, product_id, model, status, prompt, created_at)
                VALUES (?, ?, ?, 'RUNNING', ?, ?)
                """, generationId, productId, properties.openAiImageModel(), prompt, createdAt);

        try {
            List<Path> referencePaths = originals.stream().map(media::resolve).toList();
            byte[] generated = openAi.edit(referencePaths, prompt);
            byte[] finalPoster = poster.compose(generated, product);
            var stored = media.saveMarketing(productId, finalPoster);
            ProductImage image = repository.addImage(productId, "MARKETING",
                    stored.relativePath(), stored.publicUrl(), 0, false);
            jdbc.update("""
                    UPDATE ai_generations
                    SET status = 'COMPLETED', result_image_id = ?
                    WHERE id = ?
                    """, image.imageId(), generationId);
            exporter.export();
            return image;
        } catch (RuntimeException | java.io.IOException e) {
            jdbc.update("""
                    UPDATE ai_generations SET status = 'FAILED', error_message = ? WHERE id = ?
                    """, truncate(e.getMessage(), 1000), generationId);
            if (e instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Unable to save generated marketing image.", e);
        }
    }

    public List<Generation> history(String productId) {
        return jdbc.query("""
                SELECT id, product_id, model, status, result_image_id, error_message, created_at
                FROM ai_generations
                WHERE (? IS NULL OR product_id = ?)
                ORDER BY created_at DESC
                LIMIT 50
                """,
                (rs, rowNum) -> new Generation(
                        rs.getString("id"),
                        rs.getString("product_id"),
                        rs.getString("model"),
                        rs.getString("status"),
                        rs.getString("result_image_id"),
                        rs.getString("error_message"),
                        rs.getString("created_at")),
                productId, productId);
    }

    private String buildPrompt(Product product) {
        return """
                Edit the supplied real product photographs into one polished commercial catalogue photograph.

                Product: %s
                Category: %s
                Material: %s

                NON-NEGOTIABLE PRODUCT FIDELITY:
                - Preserve the actual bag exactly: silhouette, proportions, handles, zippers, seams, pockets,
                  fabric pattern, colors, printed artwork and any existing product markings.
                - Do not invent extra pockets, handles, logos, accessories, colors or construction details.
                - Do not replace the product with a similar bag.
                - If multiple reference photos are provided, use them only to understand the same product.

                ART DIRECTION:
                - Premium Indian wholesale catalogue aesthetic.
                - Warm off-white / cream studio environment with soft natural shadows.
                - Bright, professional and trustworthy; never dark themed.
                - Product should occupy the left 55-60%% of a 3:2 landscape composition.
                - Leave calm, uncluttered negative space on the right for a designed information panel.
                - Sharp realistic fabric texture, clean edge detail and believable lighting.
                - No people, hands, watermarks, decorative brand names, prices, text blocks or feature labels.
                - Do not write any text into the image. The application will add verified text later.
                """.formatted(product.name(), product.category(), product.material());
    }

    private String truncate(String value, int limit) {
        if (value == null) return "Unknown error";
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    public record Generation(
            String id,
            String productId,
            String model,
            String status,
            String resultImageId,
            String errorMessage,
            String createdAt
    ) {}
}

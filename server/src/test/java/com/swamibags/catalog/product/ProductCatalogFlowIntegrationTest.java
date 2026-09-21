package com.swamibags.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.swamibags.catalog.ai.MarketingImageService;
import com.swamibags.catalog.auth.AdminCredentialService;
import com.swamibags.catalog.media.MediaService;
import com.swamibags.catalog.settings.SiteSettingsRepository;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class ProductCatalogFlowIntegrationTest {
    private static final Path TEMP_DIR = createTempDirectory();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.admin-username", () -> "admin");
        registry.add("app.admin-password", () -> "integration-test-password");
        registry.add("app.data-dir", TEMP_DIR::toString);
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEMP_DIR.resolve("test.db"));
        registry.add("app.open-ai-api-key", () -> "");
    }

    @Autowired
    ProductService products;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    MediaService media;

    @Autowired
    SiteSettingsRepository settings;

    @Autowired
    CatalogExporter exporter;

    @Autowired
    AdminCredentialService credentials;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    MarketingImageService marketingImages;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void productCanMoveFromDraftToPublishedStaticCatalog() throws Exception {
        var request = new ProductRequest(
                "SB-TEST-01",
                "integration-test-bag",
                "Integration Test Bag",
                "Jute Bags",
                "Natural jute",
                new BigDecimal("99.00"),
                "piece",
                50,
                120,
                null,
                "15 × 14 × 5 in",
                "A test catalogue product.",
                List.of("Strong handles", "Custom printing"));

        Product created = products.create(request);
        assertThat(created.published()).isFalse();
        assertThat(created.images()).isEmpty();

        var image = new MockMultipartFile(
                "files",
                "bag.jpg",
                "image/jpeg",
                validJpegBytes());

        products.addOriginalImages(created.id(), List.of(image));
        Product published = products.setPublished(created.id(), true);

        assertThat(published.published()).isTrue();
        assertThat(published.images()).hasSize(1);
        assertThat(published.displayImage()).startsWith("/media/products/");

        Path catalog = TEMP_DIR.resolve("catalog/products.json");
        assertThat(catalog).exists();
        String catalogJson = Files.readString(catalog);
        assertThat(catalogJson).contains("SB-TEST-01");
        assertThat(catalogJson).contains("Integration Test Bag");
        assertThat(catalogJson).contains("Natural jute");
    }

    @Test
    void rejectsDisguisedImagesAndMoreThanSixReferences() throws Exception {
        var request = new ProductRequest(
                "SB-TEST-02",
                "upload-guard-test",
                "Upload Guard Test",
                "Cash Bags",
                "Polyester",
                new BigDecimal("120.00"),
                "piece",
                25,
                50,
                null,
                "12 x 9 in",
                "Upload validation test product.",
                List.of("Double zipper"));

        Product created = products.create(request);

        var disguised = new MockMultipartFile(
                "files",
                "not-really-an-image.jpg",
                "image/jpeg",
                "plain text pretending to be an image".getBytes());

        assertThatThrownBy(() -> products.addOriginalImages(created.id(), List.of(disguised)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");

        List<MultipartFile> validImages = java.util.stream.IntStream.range(0, 6)
                .mapToObj(index -> (MultipartFile) new MockMultipartFile(
                        "files",
                        "bag-" + index + ".jpg",
                        "image/jpeg",
                        validJpegBytes()))
                .toList();

        products.addOriginalImages(created.id(), validImages);
        assertThat(products.get(created.id()).images().stream().filter(ProductImage::isOriginal)).hasSize(6);

        var seventh = new MockMultipartFile(
                "files",
                "bag-7.jpg",
                "image/jpeg",
                validJpegBytes());

        assertThatThrownBy(() -> products.addOriginalImages(created.id(), List.of(seventh)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 6");
    }

    @Test
    void publishedProductCannotLoseItsFinalRealPhoto() throws Exception {
        var request = new ProductRequest(
                "SB-GUARD-01",
                null,
                "Published Guard Bag",
                "Purses",
                "PU material",
                new BigDecimal("180.00"),
                "piece",
                20,
                10,
                null,
                "",
                "",
                List.of());

        Product created = products.create(request);
        var image = new MockMultipartFile("files", "guard.jpg", "image/jpeg", validJpegBytes());
        products.addOriginalImages(created.id(), List.of(image));
        Product published = products.setPublished(created.id(), true);
        String imageId = published.images().stream().filter(ProductImage::isOriginal).findFirst().orElseThrow().imageId();

        assertThatThrownBy(() -> products.removeImage(created.id(), imageId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unpublish");
        assertThat(products.get(created.id()).images().stream().filter(ProductImage::isOriginal)).hasSize(1);

        products.setPublished(created.id(), false);
        products.removeImage(created.id(), imageId);
        assertThat(products.get(created.id()).images().stream().filter(ProductImage::isOriginal)).isEmpty();
    }

    @Test
    void productUpdatesKeepStableSlugAndRejectUnsupportedCategory() {
        var create = new ProductRequest(
                "SB-SLUG-01",
                null,
                "Stable Slug Bag",
                "Cash Bags",
                "Polyester",
                new BigDecimal("90.00"),
                "piece",
                25,
                40,
                null,
                "",
                "",
                List.of());
        Product created = products.create(create);
        String originalSlug = created.slug();

        assertThatThrownBy(() -> products.create(create))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        var rename = new ProductRequest(
                created.id(),
                null,
                "తెలుగు పేరు",
                "Cash Bags",
                "Polyester",
                new BigDecimal("90.00"),
                "piece",
                25,
                40,
                null,
                "",
                "",
                List.of());
        Product renamed = products.update(created.id(), rename);
        assertThat(renamed.slug()).isEqualTo(originalSlug);

        var second = new ProductRequest(
                "SB-SLUG-02",
                null,
                "Second Slug Bag",
                "Cash Bags",
                "Polyester",
                new BigDecimal("95.00"),
                "piece",
                25,
                40,
                null,
                "",
                "",
                List.of());
        Product secondCreated = products.create(second);
        var conflictingSlug = new ProductRequest(
                secondCreated.id(),
                originalSlug,
                secondCreated.name(),
                secondCreated.category(),
                secondCreated.material(),
                secondCreated.price(),
                secondCreated.priceUnit(),
                secondCreated.moq(),
                secondCreated.stock(),
                secondCreated.restockDays(),
                secondCreated.size(),
                secondCreated.description(),
                secondCreated.features());
        assertThatThrownBy(() -> products.update(secondCreated.id(), conflictingSlug))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slug");

        var invalidCategory = new ProductRequest(
                "SB-BAD-CAT",
                null,
                "Bad Category Bag",
                "Unknown Bags",
                "Polyester",
                BigDecimal.ZERO,
                "piece",
                1,
                0,
                null,
                "",
                "",
                List.of());
        assertThatThrownBy(() -> products.create(invalidCategory))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supported product category");
    }

    @Test
    void marketingApprovalSurvivesStockChangeButClearsWhenPosterDetailsChange() throws Exception {
        var request = new ProductRequest(
                "SB-MKT-STALE",
                null,
                "Marketing Stable Bag",
                "Luggage Bags",
                "Nylon",
                new BigDecimal("250.00"),
                "piece",
                20,
                100,
                null,
                "",
                "",
                List.of("Shoulder strap"));

        Product created = products.create(request);
        var reference = new MockMultipartFile(
                "files", "marketing-reference.jpg", "image/jpeg", validJpegBytes());
        products.addOriginalImages(created.id(), List.of(reference));

        ProductImage marketing = productRepository.addImage(
                created.id(),
                "MARKETING",
                "media/products/SB-MKT-STALE/marketing/test.jpg",
                "/media/products/SB-MKT-STALE/marketing/test.jpg",
                0,
                false);
        products.approveMarketingImage(created.id(), marketing.imageId());
        assertThat(products.get(created.id()).images().stream()
                .filter(ProductImage::isMarketing)
                .anyMatch(ProductImage::approved)).isTrue();

        var stockOnly = new ProductRequest(
                created.id(),
                null,
                created.name(),
                created.category(),
                created.material(),
                created.price(),
                created.priceUnit(),
                created.moq(),
                75,
                created.restockDays(),
                created.size(),
                created.description(),
                created.features());
        products.update(created.id(), stockOnly);
        assertThat(products.get(created.id()).images().stream()
                .filter(ProductImage::isMarketing)
                .anyMatch(ProductImage::approved)).isTrue();

        var visualChange = new ProductRequest(
                created.id(),
                null,
                "Marketing Updated Bag",
                created.category(),
                created.material(),
                created.price(),
                created.priceUnit(),
                created.moq(),
                75,
                created.restockDays(),
                created.size(),
                created.description(),
                created.features());
        products.update(created.id(), visualChange);
        assertThat(products.get(created.id()).images().stream()
                .filter(ProductImage::isMarketing)
                .anyMatch(ProductImage::approved)).isFalse();
    }

    @Test
    void unusableProductCodeFallsBackToGeneratedSafeCode() {
        var request = new ProductRequest(
                "!!!",
                null,
                "Generated Code Bag",
                "Zip Bags",
                "Polyester",
                BigDecimal.ZERO,
                "piece",
                10,
                0,
                null,
                "",
                "",
                List.of());

        Product created = products.create(request);

        assertThat(created.id()).startsWith("SB-");
        assertThat(created.id()).matches("SB-[A-Z0-9]{6}");
    }

    @Test
    void interruptedAiGenerationIsRecoveredAsFailed() throws Exception {
        var request = new ProductRequest(
                "SB-AI-RECOVERY",
                null,
                "AI Recovery Bag",
                "Jute Bags",
                "Jute",
                BigDecimal.ZERO,
                "piece",
                10,
                0,
                null,
                "",
                "",
                List.of());
        Product created = products.create(request);
        var reference = new MockMultipartFile(
                "files", "ai-reference.jpg", "image/jpeg", validJpegBytes());
        products.addOriginalImages(created.id(), List.of(reference));

        jdbc.update("""
                INSERT INTO ai_generations (id, product_id, model, status, prompt, created_at)
                VALUES (?, ?, ?, 'RUNNING', ?, ?)
                """,
                "recovery-generation",
                created.id(),
                "test-model",
                "test prompt",
                java.time.Instant.now().toString());

        assertThatThrownBy(() -> marketingImages.generate(created.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already being generated");

        marketingImages.markInterruptedGenerationsFailed();

        var state = jdbc.queryForMap(
                "SELECT status, error_message FROM ai_generations WHERE id = ?",
                "recovery-generation");
        assertThat(state.get("status")).isEqualTo("FAILED");
        assertThat(state.get("error_message").toString()).contains("interrupted");
    }

    @Test
    void brandLogoIsStoredAndExportedToPublicConfig() throws Exception {
        var logo = new MockMultipartFile(
                "file",
                "logo.jpg",
                "image/jpeg",
                validJpegBytes());

        var stored = media.saveBrandLogo(logo);
        settings.setLogoUrl(stored.publicUrl());
        exporter.export();

        assertThat(stored.publicUrl()).startsWith("/media/brand/logo-").endsWith(".jpg");
        Path storedPath = TEMP_DIR.resolve(stored.relativePath());
        assertThat(storedPath).exists();
        assertThat(Files.readString(TEMP_DIR.resolve("catalog/config.json")))
                .contains("\"logoUrl\":\"" + stored.publicUrl() + "\"");

        var replacement = media.saveBrandLogo(logo);
        Path replacementPath = TEMP_DIR.resolve(replacement.relativePath());
        assertThat(replacement.publicUrl()).isNotEqualTo(stored.publicUrl());
        assertThat(replacementPath).exists();

        media.deletePublicMediaUrl(stored.publicUrl());
        assertThat(storedPath).doesNotExist();
        assertThat(replacementPath).exists();

        media.deleteBrandLogo();
        settings.setLogoUrl("");
        exporter.export();
        assertThat(replacementPath).doesNotExist();
    }

    @Test
    void adminPasswordCanBeChangedAndPersistsInDatabase() {
        credentials.ensureBootstrap();
        assertThat(passwordEncoder.matches(
                "integration-test-password",
                credentials.loadUserByUsername("admin").getPassword())).isTrue();

        credentials.changePassword(
                "admin",
                "integration-test-password",
                "replacement-password-123");

        String changedHash = credentials.loadUserByUsername("admin").getPassword();
        assertThat(passwordEncoder.matches("replacement-password-123", changedHash)).isTrue();
        assertThat(passwordEncoder.matches("integration-test-password", changedHash)).isFalse();
    }

    private static byte[] validJpegBytes() {
        try {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Path createTempDirectory() {
        try {
            return Files.createTempDirectory("swami-bags-integration-");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}

package com.swamibags.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.mock.web.MockMultipartFile;
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

package com.swamibags.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
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
                new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9});

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

    private static Path createTempDirectory() {
        try {
            return Files.createTempDirectory("swami-bags-integration-");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}

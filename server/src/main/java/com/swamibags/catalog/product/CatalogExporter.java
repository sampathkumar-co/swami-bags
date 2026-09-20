package com.swamibags.catalog.product;

import com.swamibags.catalog.config.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CatalogExporter {
    private final ProductRepository repository;
    private final AppProperties properties;
    private final JsonMapper json;
    private final Path catalogDir;

    public CatalogExporter(ProductRepository repository, AppProperties properties, JsonMapper json) throws IOException {
        this.repository = repository;
        this.properties = properties;
        this.json = json;
        this.catalogDir = properties.dataDir().toAbsolutePath().normalize().resolve("catalog");
        Files.createDirectories(catalogDir);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        export();
    }

    public synchronized void export() {
        try {
            List<CatalogProduct> products = repository.findPublished().stream().map(this::toCatalogProduct).toList();
            writeAtomically(catalogDir.resolve("products.json"),
                    json.writeValueAsBytes(new CatalogSnapshot(Instant.now().toString(), products)));
            writeAtomically(catalogDir.resolve("config.json"),
                    json.writeValueAsBytes(new PublicConfig(
                            properties.brandName(),
                            properties.whatsappNumber(),
                            properties.businessPhone(),
                            properties.businessEmail(),
                            properties.businessAddress(),
                            properties.publicBaseUrl())));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to export public catalogue.", e);
        }
    }

    private CatalogProduct toCatalogProduct(Product product) {
        return new CatalogProduct(
                product.id(),
                product.slug(),
                product.name(),
                product.category(),
                product.material(),
                product.price(),
                product.priceUnit(),
                product.moq(),
                product.stock(),
                product.restockDays(),
                product.size(),
                product.description(),
                product.features(),
                product.displayImage(),
                product.originalImageUrls());
    }

    private void writeAtomically(Path target, byte[] content) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        Files.write(temp, content);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public record CatalogSnapshot(String generatedAt, List<CatalogProduct> products) {}

    public record CatalogProduct(
            String id,
            String slug,
            String name,
            String category,
            String material,
            java.math.BigDecimal price,
            String priceUnit,
            int moq,
            int stock,
            Integer restockDays,
            String size,
            String description,
            List<String> features,
            String image,
            List<String> images
    ) {}

    public record PublicConfig(
            String brandName,
            String whatsappNumber,
            String businessPhone,
            String businessEmail,
            String businessAddress,
            String publicBaseUrl
    ) {}
}

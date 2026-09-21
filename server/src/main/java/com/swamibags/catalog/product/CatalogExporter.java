package com.swamibags.catalog.product;

import com.swamibags.catalog.config.AppProperties;
import com.swamibags.catalog.settings.SiteSettingsRepository;
import com.swamibags.catalog.media.SharedFilePermissions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
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
    private final SiteSettingsRepository settings;
    private final JsonMapper json;
    private final Path catalogDir;

    public CatalogExporter(ProductRepository repository, AppProperties properties, SiteSettingsRepository settings, JsonMapper json) throws IOException {
        this.repository = repository;
        this.properties = properties;
        this.settings = settings;
        this.json = json;
        this.catalogDir = properties.dataDir().toAbsolutePath().normalize().resolve("catalog");
        Files.createDirectories(catalogDir);
        SharedFilePermissions.makeDirectoryPublicReadable(catalogDir);
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
            var publicSettings = settings.get();
            writeAtomically(catalogDir.resolve("config.json"),
                    json.writeValueAsBytes(new PublicConfig(
                            publicSettings.brandName(),
                            publicSettings.whatsappNumber(),
                            publicSettings.businessPhone(),
                            publicSettings.businessEmail(),
                            publicSettings.businessAddress(),
                            publicSettings.publicBaseUrl(),
                            publicSettings.logoUrl())));
            writeAtomically(catalogDir.resolve("sitemap.xml"),
                    buildSitemap(publicSettings.publicBaseUrl(), products).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to export public catalogue.", e);
        }
    }

    private String buildSitemap(String publicBaseUrl, List<CatalogProduct> products) {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
        if (!base.matches("^https?://.+")) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"></urlset>\n";
        }

        var xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        appendUrl(xml, base + "/", "1.0");
        appendUrl(xml, base + "/products", "0.9");
        appendUrl(xml, base + "/about", "0.6");
        appendUrl(xml, base + "/contact", "0.6");
        for (CatalogProduct product : products) {
            appendUrl(xml, base + "/products/" + product.slug(), "0.8");
        }
        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void appendUrl(StringBuilder xml, String url, String priority) {
        xml.append("  <url><loc>")
                .append(escapeXml(url))
                .append("</loc><priority>")
                .append(priority)
                .append("</priority></url>\n");
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
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
        try {
            Files.write(temp, content);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            SharedFilePermissions.makeFilePublicReadable(target);
        } finally {
            Files.deleteIfExists(temp);
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
            String publicBaseUrl,
            String logoUrl
    ) {}
}

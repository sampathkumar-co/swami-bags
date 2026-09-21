package com.swamibags.catalog.settings;

import com.swamibags.catalog.media.MediaService;
import com.swamibags.catalog.product.CatalogExporter;
import com.swamibags.catalog.product.ProductRepository;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {
    private final SiteSettingsRepository settings;
    private final CatalogExporter exporter;
    private final MediaService media;
    private final ProductRepository products;

    public AdminSettingsController(
            SiteSettingsRepository settings,
            CatalogExporter exporter,
            MediaService media,
            ProductRepository products) {
        this.settings = settings;
        this.exporter = exporter;
        this.media = media;
        this.products = products;
    }

    @GetMapping
    public SiteSettings get() {
        return settings.get();
    }

    @PutMapping
    public SiteSettings update(@Valid @RequestBody SiteSettings request) {
        validate(request);
        SiteSettings previous = settings.get();
        SiteSettings saved = settings.save(request);
        boolean brandChanged = !previous.brandName().equals(saved.brandName());
        List<String> previouslyApprovedMarketing = List.of();
        boolean marketingApprovalsChanged = false;
        try {
            if (brandChanged) {
                previouslyApprovedMarketing = products.approvedMarketingImageIds();
                products.unapproveAllMarketingImages();
                marketingApprovalsChanged = true;
            }
            exporter.export();
        } catch (RuntimeException updateFailure) {
            settings.save(previous);
            if (marketingApprovalsChanged) {
                products.restoreApprovedMarketingImages(previouslyApprovedMarketing);
            }
            try {
                exporter.export();
            } catch (RuntimeException rollbackFailure) {
                updateFailure.addSuppressed(rollbackFailure);
            }
            throw updateFailure;
        }
        return saved;
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SiteSettings uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        String previousUrl = settings.get().logoUrl();
        var stored = media.saveBrandLogo(file);
        try {
            SiteSettings saved = settings.setLogoUrl(stored.publicUrl());
            try {
                exporter.export();
            } catch (RuntimeException exportFailure) {
                settings.setLogoUrl(previousUrl);
                try {
                    exporter.export();
                } catch (RuntimeException rollbackFailure) {
                    exportFailure.addSuppressed(rollbackFailure);
                }
                throw exportFailure;
            }
            media.deletePublicMediaUrl(previousUrl);
            return saved;
        } catch (RuntimeException exception) {
            media.delete(stored);
            throw exception;
        }
    }

    @DeleteMapping("/logo")
    public SiteSettings deleteLogo() {
        String previousUrl = settings.get().logoUrl();
        SiteSettings saved = settings.setLogoUrl("");
        try {
            exporter.export();
        } catch (RuntimeException exportFailure) {
            settings.setLogoUrl(previousUrl);
            try {
                exporter.export();
            } catch (RuntimeException rollbackFailure) {
                exportFailure.addSuppressed(rollbackFailure);
            }
            throw exportFailure;
        }
        media.deletePublicMediaUrl(previousUrl);
        return saved;
    }

    private void validate(SiteSettings request) {
        String whatsapp = request.whatsappNumber() == null ? "" : request.whatsappNumber().replaceAll("[^0-9]", "");
        if (!whatsapp.isBlank() && (whatsapp.length() < 10 || whatsapp.length() > 15)) {
            throw new IllegalArgumentException("WhatsApp number must contain 10 to 15 digits including country code.");
        }
        String email = request.businessEmail() == null ? "" : request.businessEmail().trim();
        if (!email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid business email address.");
        }

        String baseUrl = request.publicBaseUrl() == null ? "" : request.publicBaseUrl().trim();
        if (!baseUrl.isBlank()) {
            try {
                URI uri = URI.create(baseUrl);
                boolean allowedScheme = "https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme());
                boolean rootPath = uri.getPath() == null || uri.getPath().isBlank() || "/".equals(uri.getPath());
                if (!allowedScheme || uri.getHost() == null || uri.getHost().isBlank()
                        || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null || !rootPath) {
                    throw new IllegalArgumentException("Public website URL must be a root http(s) URL with a valid host.");
                }
            } catch (IllegalArgumentException exception) {
                if ("Public website URL must be a root http(s) URL with a valid host.".equals(exception.getMessage())) {
                    throw exception;
                }
                throw new IllegalArgumentException("Enter a valid public website URL.", exception);
            }
        }
    }
}

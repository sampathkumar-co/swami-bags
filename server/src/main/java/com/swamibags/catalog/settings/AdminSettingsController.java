package com.swamibags.catalog.settings;

import com.swamibags.catalog.media.MediaService;
import com.swamibags.catalog.product.CatalogExporter;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
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

    public AdminSettingsController(SiteSettingsRepository settings, CatalogExporter exporter, MediaService media) {
        this.settings = settings;
        this.exporter = exporter;
        this.media = media;
    }

    @GetMapping
    public SiteSettings get() {
        return settings.get();
    }

    @PutMapping
    public SiteSettings update(@Valid @RequestBody SiteSettings request) {
        validate(request);
        SiteSettings saved = settings.save(request);
        exporter.export();
        return saved;
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SiteSettings uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        var stored = media.saveBrandLogo(file);
        SiteSettings saved = settings.setLogoUrl(stored.publicUrl());
        exporter.export();
        return saved;
    }

    @DeleteMapping("/logo")
    public SiteSettings deleteLogo() {
        media.deleteBrandLogo();
        SiteSettings saved = settings.setLogoUrl("");
        exporter.export();
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

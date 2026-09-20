package com.swamibags.catalog.settings;

import com.swamibags.catalog.product.CatalogExporter;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {
    private final SiteSettingsRepository settings;
    private final CatalogExporter exporter;

    public AdminSettingsController(SiteSettingsRepository settings, CatalogExporter exporter) {
        this.settings = settings;
        this.exporter = exporter;
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
        if (!baseUrl.isBlank() && !(baseUrl.startsWith("https://") || baseUrl.startsWith("http://"))) {
            throw new IllegalArgumentException("Public website URL must start with http:// or https://.");
        }
    }
}

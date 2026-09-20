package com.swamibags.catalog.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SiteSettings(
        @NotBlank @Size(max = 100) String brandName,
        @Size(max = 30) String whatsappNumber,
        @Size(max = 40) String businessPhone,
        @Size(max = 160) String businessEmail,
        @Size(max = 500) String businessAddress,
        @Size(max = 240) String publicBaseUrl,
        @Size(max = 300) String logoUrl
) {
}

package com.swamibags.catalog.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String adminUsername,
        String adminPassword,
        Path dataDir,
        String brandName,
        String whatsappNumber,
        String businessPhone,
        String businessEmail,
        String businessAddress,
        String publicBaseUrl,
        String openAiApiKey,
        String openAiImageModel,
        String openAiImageQuality,
        String openAiImageSize
) {
    public boolean aiConfigured() {
        return openAiApiKey != null && !openAiApiKey.isBlank();
    }
}

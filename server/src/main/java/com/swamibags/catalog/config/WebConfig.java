package com.swamibags.catalog.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AppProperties properties;

    public WebConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = properties.dataDir().toAbsolutePath().normalize();
        registry.addResourceHandler("/media/**")
                .addResourceLocations(root.resolve("media").toUri().toString());
        registry.addResourceHandler("/catalog/**")
                .addResourceLocations(root.resolve("catalog").toUri().toString());
    }
}

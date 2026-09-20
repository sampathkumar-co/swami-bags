package com.swamibags.catalog.settings;

import com.swamibags.catalog.config.AppProperties;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SiteSettingsRepository {
    private final JdbcTemplate jdbc;
    private final AppProperties defaults;

    public SiteSettingsRepository(JdbcTemplate jdbc, AppProperties defaults) {
        this.jdbc = jdbc;
        this.defaults = defaults;
    }

    public SiteSettings get() {
        Map<String, String> values = jdbc.query(
                "SELECT setting_key, setting_value FROM site_settings",
                (rs, rowNum) -> Map.entry(rs.getString("setting_key"), rs.getString("setting_value")))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new SiteSettings(
                value(values, "brandName", defaults.brandName()),
                value(values, "whatsappNumber", defaults.whatsappNumber()),
                value(values, "businessPhone", defaults.businessPhone()),
                value(values, "businessEmail", defaults.businessEmail()),
                value(values, "businessAddress", defaults.businessAddress()),
                value(values, "publicBaseUrl", defaults.publicBaseUrl()));
    }

    @Transactional
    public SiteSettings save(SiteSettings settings) {
        upsert("brandName", cleanRequired(settings.brandName()));
        upsert("whatsappNumber", clean(settings.whatsappNumber()));
        upsert("businessPhone", clean(settings.businessPhone()));
        upsert("businessEmail", clean(settings.businessEmail()));
        upsert("businessAddress", clean(settings.businessAddress()));
        upsert("publicBaseUrl", clean(settings.publicBaseUrl()));
        return get();
    }

    private void upsert(String key, String value) {
        jdbc.update("""
                INSERT INTO site_settings (setting_key, setting_value)
                VALUES (?, ?)
                ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value
                """, key, value);
    }

    private String value(Map<String, String> values, String key, String fallback) {
        return values.getOrDefault(key, fallback == null ? "" : fallback);
    }

    private String cleanRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Business name is required.");
        }
        return value.trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

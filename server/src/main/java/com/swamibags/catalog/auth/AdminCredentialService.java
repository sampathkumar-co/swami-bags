package com.swamibags.catalog.auth;

import com.swamibags.catalog.config.AppProperties;
import java.time.Instant;
import java.util.Locale;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOnDatabaseInitialization
public class AdminCredentialService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final AppProperties properties;
    private volatile boolean bootstrapped;

    public AdminCredentialService(JdbcTemplate jdbc, PasswordEncoder encoder, AppProperties properties) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.properties = properties;
        validateBootstrapCredentials();
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ensureBootstrap();
        return jdbc.query(
                "SELECT username, password_hash FROM admin_credentials WHERE username = ?",
                rs -> {
                    if (!rs.next()) {
                        throw new UsernameNotFoundException("Admin user not found.");
                    }
                    return User.withUsername(rs.getString("username"))
                            .password(rs.getString("password_hash"))
                            .roles("ADMIN")
                            .build();
                },
                username);
    }

    @Transactional
    public synchronized void ensureBootstrap() {
        if (bootstrapped) {
            return;
        }
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_credentials WHERE username = ?",
                Integer.class,
                properties.adminUsername());
        if (count == null || count == 0) {
            // Keep exactly one administrator. Changing ADMIN_USERNAME intentionally revokes the old account.
            jdbc.update("DELETE FROM admin_credentials");
            jdbc.update(
                    "INSERT INTO admin_credentials (username, password_hash, updated_at) VALUES (?, ?, ?)",
                    properties.adminUsername(),
                    encoder.encode(properties.adminPassword()),
                    Instant.now().toString());
        }
        bootstrapped = true;
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        ensureBootstrap();
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password is required.");
        }
        if (newPassword == null || newPassword.length() < 12 || newPassword.length() > 128) {
            throw new IllegalArgumentException("New password must be between 12 and 128 characters.");
        }

        String hash = jdbc.query(
                "SELECT password_hash FROM admin_credentials WHERE username = ?",
                rs -> rs.next() ? rs.getString("password_hash") : null,
                username);
        if (hash == null || !encoder.matches(currentPassword, hash)) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (encoder.matches(newPassword, hash)) {
            throw new IllegalArgumentException("Choose a new password that is different from the current password.");
        }

        int updated = jdbc.update(
                "UPDATE admin_credentials SET password_hash = ?, updated_at = ? WHERE username = ?",
                encoder.encode(newPassword),
                Instant.now().toString(),
                username);
        if (updated != 1) {
            throw new IllegalStateException("Unable to update the admin password.");
        }
    }

    private void validateBootstrapCredentials() {
        if (properties.adminUsername() == null || properties.adminUsername().isBlank()
                || properties.adminPassword() == null || properties.adminPassword().length() < 12) {
            throw new IllegalStateException("ADMIN_USERNAME and an ADMIN_PASSWORD of at least 12 characters are required.");
        }
        if (properties.adminPassword().toUpperCase(Locale.ROOT).contains("CHANGE_ME")) {
            throw new IllegalStateException("Replace the placeholder ADMIN_PASSWORD before starting the server.");
        }
    }
}

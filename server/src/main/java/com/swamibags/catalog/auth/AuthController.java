package com.swamibags.catalog.auth;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {
    private final AdminCredentialService credentials;

    public AuthController(AdminCredentialService credentials) {
        this.credentials = credentials;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of(
                "token", token.getToken(),
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return Map.of(
                "authenticated", authentication != null && authentication.isAuthenticated(),
                "username", authentication == null ? "" : authentication.getName());
    }

    @PostMapping("/password")
    public Map<String, Object> changePassword(
            Authentication authentication,
            @RequestBody PasswordChangeRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication required.");
        }
        credentials.changePassword(authentication.getName(), request.currentPassword(), request.newPassword());
        return Map.of("changed", true);
    }

    public record PasswordChangeRequest(String currentPassword, String newPassword) {}
}

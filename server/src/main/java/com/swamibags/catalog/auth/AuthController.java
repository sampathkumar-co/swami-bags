package com.swamibags.catalog.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
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
    private final SessionRegistry sessions;

    public AuthController(AdminCredentialService credentials, SessionRegistry sessions) {
        this.credentials = credentials;
        this.sessions = sessions;
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
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        return Map.of(
                "authenticated", authenticated,
                "username", authenticated ? authentication.getName() : "");
    }

    @PostMapping("/password")
    public Map<String, Object> changePassword(
            Authentication authentication,
            HttpServletRequest servletRequest,
            @RequestBody PasswordChangeRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication required.");
        }
        credentials.changePassword(authentication.getName(), request.currentPassword(), request.newPassword());

        var currentSession = servletRequest.getSession(false);
        String currentSessionId = currentSession == null ? "" : currentSession.getId();
        var otherSessions = sessions.getAllSessions(authentication.getPrincipal(), false).stream()
                .filter(session -> !session.getSessionId().equals(currentSessionId))
                .toList();
        otherSessions.forEach(session -> session.expireNow());

        return Map.of("changed", true, "otherSessionsRevoked", !otherSessions.isEmpty());
    }

    public record PasswordChangeRequest(String currentPassword, String newPassword) {}
}

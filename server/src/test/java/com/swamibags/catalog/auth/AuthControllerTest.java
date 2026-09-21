package com.swamibags.catalog.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

class AuthControllerTest {

    @Test
    void meReportsAnonymousRequestAsUnauthenticated() {
        AdminCredentialService credentials = mock(AdminCredentialService.class);
        SessionRegistry registry = mock(SessionRegistry.class);
        AuthController controller = new AuthController(credentials, registry);
        var anonymous = new AnonymousAuthenticationToken(
                "test-key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        var response = controller.me(anonymous);

        assertThat(response).containsEntry("authenticated", false);
        assertThat(response).containsEntry("username", "");
    }

    @Test
    void meReportsAuthenticatedAdmin() {
        AdminCredentialService credentials = mock(AdminCredentialService.class);
        SessionRegistry registry = mock(SessionRegistry.class);
        AuthController controller = new AuthController(credentials, registry);
        var admin = UsernamePasswordAuthenticationToken.authenticated(
                "admin",
                "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var response = controller.me(admin);

        assertThat(response).containsEntry("authenticated", true);
        assertThat(response).containsEntry("username", "admin");
    }

    @Test
    void passwordChangeRevokesOtherSessionsButKeepsCurrentSession() {
        AdminCredentialService credentials = mock(AdminCredentialService.class);
        SessionRegistry registry = mock(SessionRegistry.class);
        AuthController controller = new AuthController(credentials, registry);

        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin",
                "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        MockHttpServletRequest request = new MockHttpServletRequest();
        var currentHttpSession = request.getSession(true);
        SessionInformation current = new SessionInformation(
                "admin", currentHttpSession.getId(), new Date());
        SessionInformation other = new SessionInformation(
                "admin", "other-session-id", new Date());
        when(registry.getAllSessions("admin", false)).thenReturn(List.of(current, other));

        String currentValue = "current-credential-value";
        String replacementValue = "replacement-credential-value";
        var response = controller.changePassword(
                authentication,
                request,
                new AuthController.PasswordChangeRequest(currentValue, replacementValue));

        verify(credentials).changePassword("admin", currentValue, replacementValue);
        assertThat(response).containsEntry("changed", true);
        assertThat(response).containsEntry("otherSessionsRevoked", true);
        assertThat(current.isExpired()).isFalse();
        assertThat(other.isExpired()).isTrue();
    }
}

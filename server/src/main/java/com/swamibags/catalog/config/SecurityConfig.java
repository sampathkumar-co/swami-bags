package com.swamibags.catalog.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SessionRegistry sessionRegistry,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookies) throws Exception {
        var csrf = new CookieCsrfTokenRepository();
        csrf.setCookiePath("/");
        csrf.setCookieName("XSRF-TOKEN");
        csrf.setHeaderName("X-XSRF-TOKEN");
        csrf.setCookieCustomizer(cookie -> cookie
                .httpOnly(true)
                .sameSite("Strict")
                .secure(secureCookies));

        http
                .csrf(configurer -> configurer.csrfTokenRepository(csrf))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/admin/auth/csrf", "/api/admin/auth/login", "/api/admin/auth/me").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .sessionManagement(session -> {
                    session.sessionFixation(fixation -> fixation.migrateSession());
                    session.maximumSessions(-1).sessionRegistry(sessionRegistry);
                })
                .formLogin(login -> login
                        .loginProcessingUrl("/api/admin/auth/login")
                        .successHandler((request, response, authentication) ->
                                writeJson(response, HttpServletResponse.SC_OK,
                                        "{\"authenticated\":true,\"username\":\"" + escape(authentication.getName()) + "\"}"))
                        .failureHandler((request, response, exception) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "{\"authenticated\":false,\"message\":\"Invalid username or password\"}"))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/admin/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                writeJson(response, HttpServletResponse.SC_OK, "{\"authenticated\":false}"))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("SWAMI_ADMIN_SESSION", "JSESSIONID", "XSRF-TOKEN"))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) ->
                        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                "{\"message\":\"Authentication required\"}")));

        return http.build();
    }

    private static void writeJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(json);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

package com.robustcode.delivery.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.mock.env.MockEnvironment;

import com.robustcode.delivery.DeliveryPlatformApplication;
import com.robustcode.delivery.user.domain.User;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String STRONG_SECRET = "a-very-long-production-like-secret-value-0123456789";

    @Test
    void missingSecretFailsInNeonProfile() {
        MockEnvironment neon = new MockEnvironment();
        neon.setActiveProfiles("neon");

        assertThatThrownBy(() -> new JwtService("", 86_400_000L, neon))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void shortSecretFails() {
        assertThatThrownBy(() -> new JwtService("too-short", 86_400_000L, new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void devSecretIsRefusedInNeonProfile() {
        MockEnvironment neon = new MockEnvironment();
        neon.setActiveProfiles("neon");

        assertThatThrownBy(() -> new JwtService(
                JwtService.DEV_SECRET_PREFIX + "local-only-jwt-secret-change-me-0123456789", 86_400_000L, neon))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("neon");
    }

    @Test
    void neonApplicationContextDoesNotStartWithoutJwtSecret() {
        // The neon profile maps app.jwt.secret to ${JWT_SECRET:} (empty when unset).
        // Use H2 so the failure under test is the JWT check, not the database.
        SpringApplicationBuilder builder = new SpringApplicationBuilder(DeliveryPlatformApplication.class)
                .profiles("neon")
                .properties(
                        "spring.datasource.url=jdbc:h2:mem:neon-jwt-check",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=",
                        "spring.flyway.enabled=false",
                        "spring.jpa.hibernate.ddl-auto=create-drop",
                        "app.jwt.secret=",
                        "server.port=0");

        assertThatThrownBy(() -> builder.run().close())
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is missing");
    }

    @Test
    void tokenRoundTripAndTamperDetection() {
        JwtService service = new JwtService(STRONG_SECRET, 60_000L, new MockEnvironment());
        User user = User.builder().email("someone@rdp.test").role(User.Role.DRIVER).build();

        String token = service.generateToken(user);

        assertThat(service.extractUsername(token)).isEqualTo("someone@rdp.test");

        JwtService otherKey = new JwtService(STRONG_SECRET + "-other", 60_000L, new MockEnvironment());
        assertThatThrownBy(() -> otherKey.extractUsername(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService service = new JwtService(STRONG_SECRET, 1L, new MockEnvironment());
        String token = service.generateToken(User.builder().email("x@rdp.test").role(User.Role.ADMIN).build());

        Thread.sleep(1100);

        assertThatThrownBy(() -> service.extractUsername(token)).isInstanceOf(JwtException.class);
    }
}

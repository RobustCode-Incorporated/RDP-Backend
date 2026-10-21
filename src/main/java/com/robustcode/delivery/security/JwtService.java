package com.robustcode.delivery.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import com.robustcode.delivery.user.domain.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and validates JWTs.
 *
 * <p>The signing secret comes from {@code app.jwt.secret} (env {@code JWT_SECRET}).
 * The application refuses to start when the secret is missing or shorter than
 * {@value #MIN_SECRET_BYTES} bytes, and in the {@code neon} (production) profile
 * it also refuses the local development default.</p>
 */
@Service
public class JwtService {

    static final int MIN_SECRET_BYTES = 32;

    static final String DEV_SECRET_PREFIX = "dev-insecure-";

    private final SecretKey key;

    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs,
            Environment environment
    ) {
        this.key = buildKey(secret, environment);

        if (expirationMs <= 0) {
            throw new IllegalStateException("app.jwt.expiration-ms must be positive");
        }

        this.expirationMs = expirationMs;
    }

    static SecretKey buildKey(String secret, Environment environment) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is missing: set the JWT_SECRET environment variable (app.jwt.secret)"
            );
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret is too short: at least " + MIN_SECRET_BYTES + " bytes are required"
            );
        }

        if (environment != null
                && environment.acceptsProfiles(Profiles.of("neon"))
                && secret.startsWith(DEV_SECRET_PREFIX)) {
            throw new IllegalStateException(
                    "The development JWT secret cannot be used in the neon profile: set JWT_SECRET"
            );
        }

        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(User user) {

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Returns the token subject. Throws a {@link io.jsonwebtoken.JwtException}
     * (or {@link IllegalArgumentException}) if the token is invalid or expired.
     */
    public String extractUsername(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}

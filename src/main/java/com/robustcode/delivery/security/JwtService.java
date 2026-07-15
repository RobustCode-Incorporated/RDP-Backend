package com.robustcode.delivery.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.robustcode.delivery.user.domain.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


@Service
public class JwtService {


    private static final String SECRET_KEY =
            "ROBUST_DELIVERY_PLATFORM_SECRET_KEY_2026_SECURITY_AUTHENTICATION";


    private final SecretKey key =
            Keys.hmacShaKeyFor(
                    SECRET_KEY.getBytes(StandardCharsets.UTF_8)
            );



    public String generateToken(User user) {


        return Jwts.builder()

                .subject(user.getEmail())

                .claim(
                        "role",
                        user.getRole().name()
                )

                .issuedAt(
                        new Date()
                )

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                + 1000 * 60 * 60 * 24
                        )
                )

                .signWith(key)

                .compact();

    }



    public String extractUsername(String token) {


        return Jwts.parser()

                .verifyWith(key)

                .build()

                .parseSignedClaims(token)

                .getPayload()

                .getSubject();

    }

}
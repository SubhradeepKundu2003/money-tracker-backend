package com.codewithsubhra.money_tracker_backend.security.jwt;

import com.codewithsubhra.money_tracker_backend.security.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Mints and verifies the <em>internal</em> signed JWT. This token never leaves
 * the server: it is stored inside a {@code Session} row and only the opaque
 * session token is handed to the client. The JWT therefore guarantees the
 * integrity of the principal's claims server-side.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;

    public JwtService(SecurityProperties properties) {
        byte[] secret = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "app.security.jwt-secret must be at least 32 bytes for HS256 signing");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.issuer = properties.issuer();
    }

    public String issue(UUID userId, String email, String role, Instant expiresAt) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies the signature and expiry of an internal JWT.
     *
     * @return the parsed claims
     * @throws JwtException if the token is invalid, tampered with or expired
     */
    public Claims verify(String jwt) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }
}

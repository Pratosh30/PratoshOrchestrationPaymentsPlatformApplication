package com.pratosh.orchestration.security;

import com.pratosh.orchestration.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    /**
     * Generates a signed JWT for the authenticated user.
     */
    public String generateToken(User user) {

        if (user == null || user.getUsername() == null
                || user.getUsername().isBlank()) {
            throw new IllegalArgumentException(
                    "Cannot generate a JWT without a username."
            );
        }

        if (user.getId() == null || user.getRole() == null) {
            throw new IllegalArgumentException(
                    "User ID and role are required to generate a JWT."
            );
        }

        if (jwtExpirationMs <= 0) {
            throw new IllegalStateException(
                    "JWT expiration must be greater than zero."
            );
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(jwtExpirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the username after verifying the token's signature.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Validates the signature, expiration, and username.
     */
    public boolean isTokenValid(String token, String username) {

        if (token == null || token.isBlank()
                || username == null || username.isBlank()) {
            return false;
        }

        Claims claims = extractAllClaims(token);

        Date expiration = claims.getExpiration();
        String tokenUsername = claims.getSubject();

        return tokenUsername != null
                && !tokenUsername.isBlank()
                && expiration != null
                && expiration.after(new Date())
                && Objects.equals(tokenUsername, username);
    }

    /**
     * Parses a signed JWT and verifies its signature.
     */
    private Claims extractAllClaims(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT token must not be empty."
            );
        }

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Builds the HMAC signing key from a Base64-encoded secret.
     */
    private SecretKey getSigningKey() {

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT signing secret is missing from configuration."
            );
        }

        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret.trim());
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "JWT secret must be a valid Base64-encoded key.",
                    exception
            );
        }

        // HS256 requires a key of at least 256 bits (32 bytes).
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT signing key must contain at least 32 decoded bytes."
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getExpirationSeconds() {
        return jwtExpirationMs / 1000;
    }
}

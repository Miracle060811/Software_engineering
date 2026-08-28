package com.travelmate.backend.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.travelmate.common.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final long EXPIRATION = 1000L * 60 * 60 * 24;
    private final Key key;

    public JwtUtil(@Value("${app.security.jwt-secret}") String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }

        final byte[] secretBytes;
        try {
            secretBytes = Decoders.BASE64.decode(encodedSecret.trim());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET must be valid Base64", exception);
        }
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateToken(String username) {
        return generateToken(null, username, 0);
    }

    public String generateToken(Long userId, String username, Integer role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .claim("role", role == null ? 0 : role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public AuthenticatedUser extractPrincipal(String token) {
        Claims claims = parseClaims(token);
        Number userId = claims.get("uid", Number.class);
        Number role = claims.get("role", Number.class);
        return new AuthenticatedUser(
                userId == null ? null : userId.longValue(),
                claims.getSubject(),
                role == null ? 0 : role.intValue());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

package com.travelmate.backend.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.travelmate.common.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final Key key;
    private final long expirationMillis;

    public JwtUtil(String encodedSecret) {
        this(encodedSecret, 30);
    }

    @Autowired
    public JwtUtil(
            @Value("${app.security.jwt-secret}") String encodedSecret,
            @Value("${app.security.jwt-access-token-minutes:30}") long accessTokenMinutes) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        if (accessTokenMinutes < 5 || accessTokenMinutes > 1440) {
            throw new IllegalStateException("JWT_ACCESS_TOKEN_MINUTES must be between 5 and 1440");
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
        this.expirationMillis = accessTokenMinutes * 60_000L;
    }

    public String generateToken(String username) {
        return generateToken(null, username, 0, 0);
    }

    public String generateToken(Long userId, String username, Integer role) {
        return generateToken(userId, username, role, 0);
    }

    public String generateToken(Long userId, String username, Integer role, Integer tokenVersion) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(username)
                .claim("uid", userId)
                .claim("role", role == null ? 0 : role)
                .claim("ver", tokenVersion == null ? 0 : tokenVersion)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
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

    public int extractTokenVersion(String token) {
        Number tokenVersion = parseClaims(token).get("ver", Number.class);
        return tokenVersion == null ? 0 : tokenVersion.intValue();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

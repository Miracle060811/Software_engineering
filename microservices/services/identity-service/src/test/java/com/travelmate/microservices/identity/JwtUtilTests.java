package com.travelmate.microservices.identity;

import com.travelmate.backend.config.JwtUtil;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JwtUtilTests {
    private static final String SECRET = Encoders.BASE64.encode(
            "travelmate-test-secret-material-000000000000000000000000000001".getBytes(StandardCharsets.UTF_8));

    @Test
    void tokenCarriesVersionAndHasUniqueId() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);

        String first = jwtUtil.generateToken(7L, "traveler", 0, 3);
        String second = jwtUtil.generateToken(7L, "traveler", 0, 3);

        assertEquals(3, jwtUtil.extractTokenVersion(first));
        assertEquals(7L, jwtUtil.extractPrincipal(first).userId());
        assertNotEquals(first, second);
    }
}

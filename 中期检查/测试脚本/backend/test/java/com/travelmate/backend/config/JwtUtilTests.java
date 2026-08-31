package com.travelmate.backend.config;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTests {

    private static final String SHARED_SECRET = encode(
            "travelmate-test-secret-material-000000000000000000000000000001");
    private static final String OTHER_SECRET = encode(
            "travelmate-test-secret-material-000000000000000000000000000002");

    @Test
    void instancesUsingTheSameSecretAcceptEachOthersTokens() {
        JwtUtil issuer = new JwtUtil(SHARED_SECRET);
        JwtUtil verifier = new JwtUtil(SHARED_SECRET);

        String token = issuer.generateToken(42L, "alice", 1);

        assertEquals("alice", verifier.extractUsername(token));
        assertEquals(42L, verifier.extractPrincipal(token).userId());
        assertEquals(1, verifier.extractPrincipal(token).role());
    }

    @Test
    void aDifferentSecretCannotVerifyTheToken() {
        JwtUtil issuer = new JwtUtil(SHARED_SECRET);
        JwtUtil verifier = new JwtUtil(OTHER_SECRET);

        assertThrows(JwtException.class, () -> verifier.extractUsername(issuer.generateToken("alice")));
    }

    @Test
    void missingOrWeakSecretsAreRejected() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil(""));
        assertThrows(IllegalStateException.class, () -> new JwtUtil(encode("too-short")));
        assertThrows(IllegalStateException.class, () -> new JwtUtil("not valid base64%%%"));
    }

    private static String encode(String value) {
        return Encoders.BASE64.encode(value.getBytes(StandardCharsets.UTF_8));
    }
}


package com.shivani.shop_api.auth;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // Generate a fresh 256-bit key for testing
    private String createTestSecret() {

        byte[] keyBytes = new byte[32];

        new SecureRandom().nextBytes(keyBytes);

        return Base64.getEncoder()
                .encodeToString(keyBytes);
    }

    // TEST 1: Generate and verify a valid JWT
    @Test
    void shouldGenerateAndValidateToken() {

        JwtService jwtService = new JwtService(
                createTestSecret(),
                3600000
        );

        String email = "rahul@example.com";

        String token = jwtService.generateToken(email);

        assertNotNull(token);

        assertEquals(
                email,
                jwtService.extractEmail(token)
        );

        assertTrue(
                jwtService.isTokenValid(token, email)
        );

        assertFalse(
                jwtService.isTokenValid(
                        token,
                        "another@example.com"
                )
        );
    }

    // TEST 2: Reject a token signed with another key
    @Test
    void shouldRejectInvalidSignature() {

        JwtService firstService = new JwtService(
                createTestSecret(),
                3600000
        );

        JwtService secondService = new JwtService(
                createTestSecret(),
                3600000
        );

        String email = "rahul@example.com";

        String token = firstService.generateToken(email);

        assertFalse(
                secondService.isTokenValid(token, email)
        );
    }

    // TEST 3: Reject an expired token
    @Test
    void shouldRejectExpiredToken() {

        JwtService jwtService = new JwtService(
                createTestSecret(),
                -60000
        );

        String email = "rahul@example.com";

        String token = jwtService.generateToken(email);

        assertFalse(
                jwtService.isTokenValid(token, email)
        );
    }
}

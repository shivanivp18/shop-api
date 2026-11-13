
package com.shivani.shop_api.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {

        byte[] keyBytes = Decoders.BASE64.decode(secret);

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    // Generate a signed JWT token
    public String generateToken(String email) {

        Date issuedAt = new Date();

        Date expiresAt = new Date(
                issuedAt.getTime() + expirationMs
        );

        return Jwts.builder()
                .subject(email)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    // Verify a token and extract the customer's email
    public String extractEmail(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Check whether a token is valid for this customer
    public boolean isTokenValid(String token, String email) {

        try {
            String tokenEmail = extractEmail(token);

            return tokenEmail != null
                    && tokenEmail.equals(email);

        } catch (JwtException | IllegalArgumentException e) {
            return false; 
        }
    }
}

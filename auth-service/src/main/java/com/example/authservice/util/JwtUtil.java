package com.example.authservice.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class  JwtUtil {

    // Secret key used for signing and verifying JWT tokens
    private final Key secretKey;

    /**
     * Constructor that initializes the JWT utility with a secret key
     * @param secret Base64-encoded secret string from application configuration
     */
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // Decode the Base64-encoded secret string into raw bytes
        byte[] keyBytes =
                Base64.getDecoder()
                        .decode(secret.getBytes(StandardCharsets.UTF_8));

        // Create an HMAC SHA key suitable for JWT signing/verification
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT (JSON Web Token) for a given user email and role.
     *
     * @param email the user's email (used as the token subject)
     * @param role  the user's role (added as a claim)
     * @return a signed JWT string
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                // Set the email as the subject of the token
                .subject(email)

                // Add a custom claim (user role) to the token payload
                .claim("role", role)

                // Set the token issue time
                .issuedAt(new Date())

                // Set the token expiration time (valid for 10 hours)
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))

                // Sign the token using the secret key
                .signWith(secretKey)

                // Build and return the compact JWT string
                .compact();
    }

    public void validateToken(String token) {
        try {
            // 1. Configure the parser with the signing key.
            Jwts.parser()
                    .verifyWith((SecretKey) secretKey)
                    .build()

                    // 2. Parse and validate the token.
                    // This single call performs signature verification AND checks standard claims (like expiration).
                    .parseSignedClaims(token);

        } catch (SignatureException e) {
            // Handles cases where the signature doesn't match the key (token tampered with or wrong key).
            throw new JwtException("Invalid JWT signature", e);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Handles cases where the 'exp' (Expiration) claim is in the past.
            throw new JwtException("JWT token has expired", e);
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            // Handles cases where the token is not a valid JWT format (e.g., wrong number of segments).
            throw new JwtException("Malformed JWT token", e);
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            // Handles tokens signed with an algorithm not supported by the parser.
            throw new JwtException("Unsupported JWT token", e);
//        } catch (io.jsonwebtoken.PrematurelySignedException e) {
//            // Handles cases where the 'nbf' (Not Before) claim is in the future.
//            throw new JwtException("JWT token is not yet valid", e);
        } catch (IllegalArgumentException e) {
            // Handles cases where the token string is null or empty.
            throw new JwtException("JWT token string cannot be null or empty", e);
        }
    }
}

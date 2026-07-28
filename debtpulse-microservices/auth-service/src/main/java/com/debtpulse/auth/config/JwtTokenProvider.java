package com.debtpulse.auth.config;

import com.debtpulse.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Issues the platform JWT. The signing secret and expiry come from the Config Server
 * (shared with the API Gateway, which validates the token). Claims carry role, branchId
 * and name so the gateway can forward identity headers without any DB lookup.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expiryMs;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiry-ms}") long expiryMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiryMs = expiryMs;
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getUserId())
                .claim("role", user.getRole().name())
                .claim("branchId", user.getBranchId())
                .claim("name", user.getFullName())
                .claim("email", user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

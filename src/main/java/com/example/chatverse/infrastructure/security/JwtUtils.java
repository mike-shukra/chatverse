package com.example.chatverse.infrastructure.security;

import com.example.chatverse.domain.service.AuthService;
import com.example.chatverse.infrastructure.configuration.SecurityConfigLoader;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);
    private final Key secretKey;

    public JwtUtils(SecurityConfigLoader configLoader) {
        String base64Secret = configLoader.getSecretKey();
        byte[] decodedSecret = Base64.getDecoder().decode(base64Secret);
        this.secretKey = Keys.hmacShaKeyFor(decodedSecret);
    }

    public String extractUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String generateToken(String userId, long expirationTime) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true; // Если токен успешно распарсен, он валиден
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false; // Если произошло исключение, токен не валиден
        }
    }

    public String getSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}

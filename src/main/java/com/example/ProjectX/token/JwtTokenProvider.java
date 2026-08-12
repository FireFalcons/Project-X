package com.example.ProjectX.token;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {
    private static final String SECRET_KEY = "52345791264134471928072630816285";
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;


    public String generateToken(String email, String role) {
        return Jwts.builder()
                    .subject(email)
                    .claim("role", role)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                    .signWith(getSigningKey())
                    .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

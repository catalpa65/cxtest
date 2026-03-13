package com.example.ecommerce.common.auth;

import com.example.ecommerce.modules.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expireSeconds;

    public JwtTokenProvider(
            @Value("${app.security.jwt-secret}") String jwtSecret,
            @Value("${app.security.jwt-expire-seconds}") long expireSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("uid", user.getId())
                .claim("email", user.getEmail())
                .claim("nickname", user.getNickname())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public UserPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = claims.get("uid", Long.class);
        String email = claims.get("email", String.class);
        String nickname = claims.get("nickname", String.class);
        return new UserPrincipal(userId, email, nickname);
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }
}

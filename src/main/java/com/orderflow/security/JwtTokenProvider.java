package com.orderflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    @Value("${orderflow.jwt.secret}")
    private String secret;
    @Value("${orderflow.jwt.expiration-minutes:720}")
    private long expirationMinutes;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // 要求密钥长度 >= 256 bit
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(LoginUser user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMinutes * 60_000L);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", String.valueOf(user.getUserId()))
                .claim("tenantId", String.valueOf(user.getTenantId()))
                .claim("roles", user.getRoles() == null ? List.of() : user.getRoles())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public LoginUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        List<?> rawRoles = claims.get("roles", List.class);
        List<String> roles = rawRoles == null ? new ArrayList<>() : rawRoles.stream()
                .map(String::valueOf).toList();
        return LoginUser.builder()
                .username(claims.getSubject())
                .userId(Long.valueOf(claims.get("userId", String.class)))
                .tenantId(Long.valueOf(claims.get("tenantId", String.class)))
                .roles(roles)
                .build();
    }

    public boolean validate(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

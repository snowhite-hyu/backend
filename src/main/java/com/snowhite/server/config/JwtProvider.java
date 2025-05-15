package com.snowhite.server.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;
    private SecretKey key;
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 7 ;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }


    // 토큰 사용자 속성 정보
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build().parseClaimsJws(token).getBody();
    }


    // 토큰 생성
    public String generateToken(final long userId) {
        return generateToken(userId, new HashMap<>());
    }

    public String generateToken(final long userId, final Map<String, Object> claims)
    {
        return Jwts.builder()
                .signWith(key)
                .setClaims(claims)
                .setId(String.valueOf(userId))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .compact();
    }


    // 토큰 검증
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return false;
    }


    // 토큰 기간 확인
    public Map<String, LocalDateTime> extractTokenTimes(String token) {
        Claims claims = extractAllClaims(token);

        LocalDateTime issuedAt = toLocalDateTime(claims.getIssuedAt());
        LocalDateTime expiration = toLocalDateTime(claims.getExpiration());
        LocalDateTime now = LocalDateTime.now();

        Map<String, LocalDateTime> timeInfo = new HashMap<>();
        timeInfo.put("issuedAt", issuedAt);
        timeInfo.put("expiration", expiration);
        timeInfo.put("now", now);
        return timeInfo;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public long extractUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);

        return Long.parseLong(claims.getId());
    }

}
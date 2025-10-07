package com.charbel.backend.service;

import com.charbel.backend.config.AppProps;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.time.Clock;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;

@Service
public class JWTService {
    private final AppProps props;
    private final Clock clock;

    @Autowired
    public JWTService(AppProps props) {
        this(props, Clock.systemUTC());
    }

    public JWTService(AppProps props, Clock clock) {
        this.props = Objects.requireNonNull(props);
        this.clock = Objects.requireNonNull(clock);

        ensureStrongKey();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(props.getJwt().getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private void ensureStrongKey() {
        byte[] keyBytes = Decoders.BASE64.decode(props.getJwt().getSecret());
        if(keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret doit être une clé Base64 d'au moins 256 bits (32 octets).");
        }
    }

    private long expirationMs() {
        return props.getJwt().getExpirationMs() != null ? props.getJwt().getExpirationMs() : 24 * 60 * 60 * 1000L;
    }

    public String generateToken(String subjectEmail) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subjectEmail)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

            return claimsResolver.apply(claims);
        }
        catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("JWT invalide: " + ex.getMessage(), ex);
        }
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        try {
            String subject = extractEmail(token);
            return expectedUsername.equalsIgnoreCase(subject) && !isTokenExpired(token);
        }
        catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date exp = extractClaim(token, Claims::getExpiration);
        return exp.before(new Date());
    }

    public boolean isAboutToExpire(String token, long thresholdMs) {
        Date exp = extractClaim(token, Claims::getExpiration);
        long remaining = exp.getTime() - clock.millis();
        return remaining <= thresholdMs;
    }
}
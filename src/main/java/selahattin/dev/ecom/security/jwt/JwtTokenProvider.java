package selahattin.dev.ecom.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.config.JwtProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    // --- Key Generation ---
    private SecretKey getAccessSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getAccessSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getRefreshSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getRefreshSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    // --- Generate Token ---

    public String generateAccessToken(UserDetails userDetails) {
        long expiration = jwtProperties.getAccessTokenExpirationMs();

        String role = userDetails.getAuthorities().isEmpty()
                ? "USER"
                : userDetails.getAuthorities().iterator().next().getAuthority();

        return generateToken(userDetails.getUsername(), role, expiration, getAccessSigningKey());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        long expiration = jwtProperties.getRefreshTokenExpirationMs();
        // Refresh token'da rol claim'ine gerek yok, null geçiyoruz
        return generateToken(userDetails.getUsername(), null, expiration, getRefreshSigningKey());
    }

    // Ortak private metot (Kod tekrarını önler)
    private String generateToken(String subject, String role, long expirationMillis, SecretKey key) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    // --- Validate Token ---

    public boolean validateAccessToken(String token) {
        return validateToken(token, getAccessSigningKey());
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, getRefreshSigningKey());
    }

    private boolean validateToken(String token, SecretKey key) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT Error: {}", e.getMessage());
            return false;
        }
    }

    // --- Extract Data ---

    public String extractUsername(String token, boolean isAccessToken) {
        SecretKey key = isAccessToken ? getAccessSigningKey() : getRefreshSigningKey();
        return extractAllClaims(token, key).getSubject();
    }

    public String extractRole(String token) {
        try {
            Claims claims = extractAllClaims(token, getAccessSigningKey());
            String role = claims.get("role", String.class);
            return (role == null) ? "USER" : role;
        } catch (Exception e) {
            return "USER";
        }
    }

    private Claims extractAllClaims(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
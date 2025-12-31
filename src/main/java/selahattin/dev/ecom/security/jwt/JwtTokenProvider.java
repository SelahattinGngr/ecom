package selahattin.dev.ecom.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.config.properties.JwtProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    // --- Keys ---
    private SecretKey getAccessSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getAccessSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getRefreshSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getRefreshSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    // --- Generate Access Token ---
    public String generateAccessToken(String email, String userId, List<String> roles, String deviceId) {
        long expiration = jwtProperties.getAccessTokenExpirationMs();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getAccessSigningKey(), Jwts.SIG.HS256)
                .claim("uid", userId)
                .claim("did", deviceId)
                .claim("roles", roles)
                .compact();
    }

    // --- Generate Refresh Token ---
    public String generateRefreshToken(String email, String deviceId) {
        long expiration = jwtProperties.getRefreshTokenExpirationMs();
        return Jwts.builder()
                .subject(email)
                .claim("did", deviceId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getRefreshSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // --- Validations ---
    public boolean validateAccessToken(String token) {
        return validateToken(token, getAccessSigningKey());
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, getRefreshSigningKey());
    }

    private boolean validateToken(String token, SecretKey key) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT Error: {}", e.getMessage());
            return false;
        }
    }

    // --- Extractions ---
    public String extractUsername(String token, boolean isAccessToken) {
        return extractAllClaims(token, isAccessToken ? getAccessSigningKey() : getRefreshSigningKey()).getSubject();
    }

    public String extractDeviceId(String token, boolean isAccessToken) {
        return extractAllClaims(token, isAccessToken ? getAccessSigningKey() : getRefreshSigningKey())
                .get("did", String.class);
    }

    public List<String> extractRoles(String token) {
        List<String> roles = extractAllClaims(token, getAccessSigningKey()).get("roles", List.class);
        return roles;
    }

    public String extractUserId(String token) {
        return extractAllClaims(token, getAccessSigningKey()).get("uid", String.class);
    }

    private Claims extractAllClaims(String token, SecretKey key) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
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
import selahattin.dev.ecom.config.properties.JwtProperties;
import selahattin.dev.ecom.entity.UserEntity;
import selahattin.dev.ecom.security.CustomUserDetails;

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

    public String generateAccessToken(CustomUserDetails userDetails) {
        long expiration = jwtProperties.getAccessTokenExpirationMs();
        UserEntity user = userDetails.getUser();

        String role = userDetails.getAuthorities().isEmpty()
                ? "USER"
                : userDetails.getAuthorities().iterator().next().getAuthority();

        String uid = user.getId().toString();
        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        return generateToken(
                userDetails.getUsername(),
                role,
                uid,
                firstName,
                lastName,
                expiration,
                getAccessSigningKey());
    }

    public String generateRefreshToken(UserDetails userDetails, String deviceId) {
        long expiration = jwtProperties.getRefreshTokenExpirationMs();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("did", deviceId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getRefreshSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // Helper Metot (Payload Zenginleştirme)
    private String generateToken(String subject, String role, String uid, String firstName, String lastName,
            long expirationMillis, SecretKey key) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256)
                .claim("role", role)
                .claim("uid", uid);

        if (firstName != null)
            builder.claim("fn", firstName);
        if (lastName != null)
            builder.claim("ln", lastName);

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

    public String extractFirstName(String token) {
        return extractClaim(token, "fn");
    }

    public String extractLastName(String token) {
        return extractClaim(token, "ln");
    }

    public String extractUserId(String jwt) {
        return extractClaim(jwt, "uid");
    }

    public String extractDeviceId(String token) {
        try {
            Claims claims = extractAllClaims(token, getRefreshSigningKey());
            return claims.get("did", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractClaim(String token, String key) {
        try {
            Claims claims = extractAllClaims(token, getAccessSigningKey());
            return claims.get(key, String.class);
        } catch (Exception e) {
            return null;
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
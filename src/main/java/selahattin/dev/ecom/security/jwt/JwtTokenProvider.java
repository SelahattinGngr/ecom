package selahattin.dev.ecom.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.config.properties.JwtProperties;
import selahattin.dev.ecom.dto.infra.TokenDto;
import selahattin.dev.ecom.entity.auth.UserEntity;
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

        // Rolleri String listesine çevir
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String uid = user.getId().toString();
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String phoneNumber = user.getPhoneNumber();

        return generateToken(TokenDto.builder()
                .subject(userDetails.getUsername())
                .roles(roles)
                .uid(uid)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .expirationMillis(expiration)
                .key(getAccessSigningKey())
                .build());
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

    // Helper Metot (Payload Doldurma)
    private String generateToken(TokenDto tokenDto) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenDto.getExpirationMillis());

        var builder = Jwts.builder()
                .subject(tokenDto.getSubject()) // email
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(tokenDto.getKey(), Jwts.SIG.HS256)
                .claim("roles", tokenDto.getRoles()) // Rol Listesi
                .claim("uid", tokenDto.getUid()); // User ID

        if (tokenDto.getFirstName() != null)
            builder.claim("fn", tokenDto.getFirstName());
        if (tokenDto.getLastName() != null)
            builder.claim("ln", tokenDto.getLastName());
        if (tokenDto.getPhoneNumber() != null)
            builder.claim("pn", tokenDto.getPhoneNumber());

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

    public List<String> extractRoles(String token) {
        try {
            Claims claims = extractAllClaims(token, getAccessSigningKey());
            List<?> rawList = claims.get("roles", List.class);
            if (rawList == null)
                return Collections.emptyList();

            return rawList.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public String extractFirstName(String token) {
        return extractClaim(token, "fn");
    }

    public String extractLastName(String token) {
        return extractClaim(token, "ln");
    }

    public String extractPhoneNumber(String token) {
        return extractClaim(token, "pn");
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

    public class JwtTokenException extends RuntimeException {
        public JwtTokenException(String message) {
            super(message);
        }
    }
}

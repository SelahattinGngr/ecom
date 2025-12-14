package selahattin.dev.ecom.service.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenStoreService {

    private final StringRedisTemplate redisTemplate;
    private static final String TOKEN_PREFIX = "auth:refresh:";

    private String buildKey(String username, String deviceId) {
        return TOKEN_PREFIX + username + ":" + deviceId;
    }

    /**
     * Token'ı Hashleyerek Sakla
     */
    public void storeToken(String username, String deviceId, String rawToken, long expirationMillis) {
        String hashedToken = hashToken(rawToken);
        redisTemplate.opsForValue().set(
                buildKey(username, deviceId),
                hashedToken,
                Duration.ofMillis(expirationMillis));
    }

    /**
     * Token Geçerli mi? (Hash Kontrolü)
     * Gelen raw token'ı hashleyip, Redis'teki hash ile kıyaslıyoruz.
     */
    public boolean validateToken(String username, String deviceId, String rawToken) {
        String storedHash = redisTemplate.opsForValue().get(buildKey(username, deviceId));

        if (storedHash == null) {
            return false;
        }

        String incomingHash = hashToken(rawToken);
        return storedHash.equals(incomingHash);
    }

    public void deleteToken(String username, String deviceId) {
        redisTemplate.delete(buildKey(username, deviceId));
    }

    public void deleteAllTokensForUser(String username) {
        String matchPattern = TOKEN_PREFIX + username + ":*";
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(matchPattern).count(100).build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } catch (Exception e) {
            log.error("Redis scan hatası: {}", e.getMessage());
        }

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * SHA-256 Hashing Helper
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algoritması bulunamadı!", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
package selahattin.dev.ecom.service.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.dto.infra.SessionPayload;
import selahattin.dev.ecom.dto.response.SessionResponse;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenStoreService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOKEN_PREFIX = "auth:refresh:";

    private String buildKey(String username, String deviceId) {
        return TOKEN_PREFIX + username + ":" + deviceId;
    }

    /**
     * Session'ı (Kullanıcı Verileri + Metadata) Redis'e kaydet
     */
    public void storeSession(String username, String deviceId, SessionPayload payload, long expirationMillis) {
        String rawToken = payload.getHashedRefreshToken();
        String finalHash = hashToken(rawToken);
        payload.setHashedRefreshToken(finalHash);
        payload.setLastActiveAt(System.currentTimeMillis());

        try {
            String jsonValue = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(
                    buildKey(username, deviceId),
                    jsonValue,
                    Duration.ofMillis(expirationMillis));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.JSON_PROCESSING_ERROR, "Oturum verisi kaydedilemedi.");
        }
    }

    /**
     * Filter için Session Bilgisini Çek (Veritabanına gitmemek için)
     */
    public SessionPayload getSession(String username, String deviceId) {
        String jsonValue = redisTemplate.opsForValue().get(buildKey(username, deviceId));
        if (jsonValue == null) {
            return null;
        }

        try {
            return objectMapper.readValue(jsonValue, SessionPayload.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Token Geçerli mi? (Payload içindeki hash ile kıyasla)
     */
    public boolean validateToken(String username, String deviceId, String rawToken) {
        SessionPayload session = getSession(username, deviceId);
        if (session == null) {
            return false;
        }
        String incomingHash = hashToken(rawToken);
        session.setLastActiveAt(System.currentTimeMillis());
        return session.getHashedRefreshToken().equals(incomingHash);
    }

    /**
     * Kullanıcının Tüm Oturumlarını Listele.
     * Blokaj yaratan KEYS yerine non-blocking SCAN kullanır.
     */
    public List<SessionResponse> getUserSessions(String email, String currentDeviceId) {
        String pattern = TOKEN_PREFIX + email + ":*";
        List<String> keys = scanKeys(pattern);
        List<SessionResponse> sessions = new ArrayList<>();

        if (keys.isEmpty()) {
            return sessions;
        }

        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) {
            return sessions;
        }

        for (int i = 0; i < keys.size(); i++) {
            String jsonValue = values.get(i);
            if (jsonValue == null) {
                continue;
            }

            String key = keys.get(i);
            String deviceId = key.substring(key.lastIndexOf(":") + 1);

            try {
                SessionPayload stored = objectMapper.readValue(jsonValue, SessionPayload.class);
                sessions.add(SessionResponse.builder()
                        .deviceId(deviceId)
                        .ipAddress(stored.getIpAddress())
                        .userAgent(stored.getUserAgent())
                        .lastActiveAt(stored.getLastActiveAt())
                        .isCurrent(deviceId.equals(currentDeviceId))
                        .build());
            } catch (JsonProcessingException e) {
                log.error("Session listeleme hatası. Key: {}", key, e);
            }
        }
        return sessions;
    }

    public void deleteToken(String username, String deviceId) {
        redisTemplate.delete(buildKey(username, deviceId));
    }

    /**
     * Kullanıcının tüm oturumlarını sil.
     * Blokaj yaratan KEYS yerine non-blocking SCAN kullanır.
     */
    public void deleteAllTokensForUser(String username) {
        List<String> keys = scanKeys(TOKEN_PREFIX + username + ":*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Redis SCAN komutu ile verilen pattern'a uyan key'leri döner.
     *
     * KEYS komutu O(N) ve single-threaded Redis'i tamamen kilitler.
     * SCAN ise cursor tabanlı, non-blocking ve iteratif çalışır —
     * count(100) her adımda yaklaşık 100 key işler ve control'ü geri verir.
     */
    private List<String> scanKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        } catch (Exception e) {
            log.error("Redis SCAN hatası. Pattern: {}", pattern, e);
        }
        return keys;
    }

    // --- SHA-256 Hashing Helper ---

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.CRYPTO_ERROR, "Hashing algoritması bulunamadı.");
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

package selahattin.dev.ecom.service.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

        // Son aktiflik zamanını güncelle
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
     * Kullanıcının Tüm Oturumlarını Listele
     */
    public List<SessionResponse> getUserSessions(String email, String currentDeviceId) {
        String pattern = TOKEN_PREFIX + email + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        List<SessionResponse> sessions = new ArrayList<>();

        if (keys == null || keys.isEmpty())
            return sessions;

        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        if (values == null)
            return sessions;

        int i = 0;
        for (String key : keys) {
            String jsonValue = values.get(i++);
            if (jsonValue == null)
                continue;

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
                log.error("Session listeleme hatası", e);
            }
        }
        return sessions;
    }

    public void deleteToken(String username, String deviceId) {
        redisTemplate.delete(buildKey(username, deviceId));
    }

    public void deleteAllTokensForUser(String username) {
        Set<String> keys = redisTemplate.keys(TOKEN_PREFIX + username + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // SHA-256 Hashing Helper
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.CRYPTO_ERROR, "Hashing algoritması bulunamadı.");
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
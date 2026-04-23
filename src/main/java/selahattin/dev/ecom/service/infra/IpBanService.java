package selahattin.dev.ecom.service.infra;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IpBanService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String COUNT_PREFIX = "ipban:count:";
    private static final String BANNED_PREFIX = "ipban:banned:";

    private static final int BAD_REQUEST_THRESHOLD = 20;   // 10 dakikada 20 hatalı istek
    private static final int COUNT_WINDOW_MINUTES = 10;
    private static final long DEFAULT_BAN_HOURS = 24;

    public boolean isBanned(String ip) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BANNED_PREFIX + ip));
    }

    public void recordBadRequest(String ip) {
        String countKey = COUNT_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1) {
            redisTemplate.expire(countKey, COUNT_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        if (count != null && count >= BAD_REQUEST_THRESHOLD) {
            ban(ip, DEFAULT_BAN_HOURS, TimeUnit.HOURS);
            redisTemplate.delete(countKey);
            log.warn("[IPBAN] IP banlandı: {} — {} dakikada {} hatalı istek", ip, COUNT_WINDOW_MINUTES, count);
        }
    }

    public void ban(String ip, long duration, TimeUnit unit) {
        redisTemplate.opsForValue().set(BANNED_PREFIX + ip, "1", duration, unit);
    }

    public void unban(String ip) {
        redisTemplate.delete(BANNED_PREFIX + ip);
        redisTemplate.delete(COUNT_PREFIX + ip);
        log.info("[IPBAN] IP ban kaldırıldı: {}", ip);
    }
}

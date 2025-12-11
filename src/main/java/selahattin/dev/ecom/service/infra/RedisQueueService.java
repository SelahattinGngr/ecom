package selahattin.dev.ecom.service.infra;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.infra.EmailMessageDto;

@Service
@RequiredArgsConstructor
public class RedisQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    public static final String EMAIL_QUEUE = "email_queue";

    public void enqueueEmail(EmailMessageDto emailDto) {
        redisTemplate.opsForList().leftPush(EMAIL_QUEUE, emailDto);
    }
}
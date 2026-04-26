package selahattin.dev.ecom.service.infra;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.dto.infra.ActivityLogDto;
import selahattin.dev.ecom.entity.audit.UserActivityEventEntity;
import selahattin.dev.ecom.repository.audit.UserActivityEventRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLogQueueListener {

    private static final int BATCH_SIZE = 50;

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserActivityEventRepository repository;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean active = true;

    @PostConstruct
    public void startListening() {
        executorService.submit(this::listen);
    }

    @PreDestroy
    public void stopListening() {
        log.warn("Uygulama kapanıyor, RequestLog dinleyicisi durduruluyor...");
        active = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void listen() {
        log.info("Redis RequestLog Kuyruğu dinleniyor...");
        while (active) {
            try {
                if (Thread.currentThread().isInterrupted()) break;

                // Block up to 5s waiting for first item — natural flush interval
                ActivityLogDto first = (ActivityLogDto) redisTemplate.opsForList()
                        .rightPop(RedisQueueService.REQUEST_LOG_QUEUE, Duration.ofSeconds(5));

                if (first == null) continue;

                List<ActivityLogDto> batch = new ArrayList<>(BATCH_SIZE);
                batch.add(first);

                // Drain remaining items non-blocking up to batch limit
                while (batch.size() < BATCH_SIZE) {
                    ActivityLogDto next = (ActivityLogDto) redisTemplate.opsForList()
                            .rightPop(RedisQueueService.REQUEST_LOG_QUEUE);
                    if (next == null) break;
                    batch.add(next);
                }

                flush(batch);

            } catch (Exception e) {
                if (active) {
                    log.error("RequestLog kuyruğu işlenirken hata: {}", e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.info("RequestLog dinleyicisi durdu.");
    }

    private void flush(List<ActivityLogDto> batch) {
        List<UserActivityEventEntity> entities = batch.stream()
                .map(dto -> UserActivityEventEntity.builder()
                        .userId(dto.getUserId())
                        .deviceId(dto.getDeviceId())
                        .ipAddress(dto.getIpAddress())
                        .method(dto.getMethod())
                        .endpoint(dto.getEndpoint())
                        .statusCode(dto.getStatusCode())
                        .createdAt(dto.getTimestamp())
                        .build())
                .toList();

        repository.saveAll(entities);
        log.debug("RequestLog batch flushed: {} kayıt", entities.size());
    }
}

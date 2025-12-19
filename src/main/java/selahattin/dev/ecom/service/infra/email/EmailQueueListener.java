package selahattin.dev.ecom.service.infra.email;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.dto.infra.EmailMessageDto;
import selahattin.dev.ecom.service.infra.RedisQueueService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailQueueListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private volatile boolean active = true;

    @PostConstruct
    public void startListening() {
        executorService.submit(this::listen);
    }

    // Bean yok edilirken çalışacak metot
    @PreDestroy
    public void stopListening() {
        log.warn("Uygulama kapanıyor, Email dinleyicisi durduruluyor...");
        this.active = false; // Döngüyü kır
        executorService.shutdown(); // Havuzu kapat
        try {
            // Mevcut işin bitmesi için kısa bir süre tanı
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Zorla kapat
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void listen() {
        log.info("Redis Email Kuyruğu dinleniyor...");
        while (active) {
            try {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                EmailMessageDto emailDto = (EmailMessageDto) redisTemplate.opsForList()
                        .rightPop(RedisQueueService.EMAIL_QUEUE, Duration.ofSeconds(5));

                if (emailDto != null) {
                    log.info("Kuyruktan mail alındı: {}", emailDto.getTo());
                    emailService.sendMail(
                            emailDto.getTo(),
                            emailDto.getSubject(),
                            emailDto.getContent());
                }
            } catch (Exception e) {
                if (active) {
                    log.error("Kuyruk işlenirken hata oluştu: {}", e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.info("Email dinleyicisi durdu.");
    }
}
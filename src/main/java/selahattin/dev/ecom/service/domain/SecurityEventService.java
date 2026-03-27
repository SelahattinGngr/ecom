package selahattin.dev.ecom.service.domain;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.entity.audit.SecurityEventEntity;
import selahattin.dev.ecom.repository.audit.SecurityEventRepository;
import selahattin.dev.ecom.utils.enums.SecurityEventType;

/**
 * Güvenlik olaylarını veritabanına yazar.
 *
 * REQUIRES_NEW: Dışarıdaki transaction'dan bağımsız commit edilir.
 * Örneğin LOGIN_FAILED exception fırlatılsa bile log kaydı korunur.
 * Yazma hatası dış akışı kesmemesi için çağrı noktalarında try-catch ile sarılır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityEventService {

    private final SecurityEventRepository securityEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(SecurityEventType eventType,
                    UUID userId,
                    String email,
                    String ipAddress,
                    String userAgent,
                    Map<String, Object> metadata) {

        SecurityEventEntity event = SecurityEventEntity.builder()
                .eventType(eventType.name())
                .userId(userId)
                .email(email)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .metadata(metadata)
                .build();

        securityEventRepository.save(event);
    }
}

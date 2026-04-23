package selahattin.dev.ecom.service.infra;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.entity.audit.UserActivityEventEntity;
import selahattin.dev.ecom.repository.audit.UserActivityEventRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityEventService {

    private final UserActivityEventRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID userId, String deviceId, String ipAddress, String method, String endpoint, int statusCode) {
        try {
            repository.save(UserActivityEventEntity.builder()
                    .userId(userId)
                    .deviceId(deviceId)
                    .ipAddress(ipAddress)
                    .method(method)
                    .endpoint(endpoint)
                    .statusCode(statusCode)
                    .build());
        } catch (Exception e) {
            log.warn("[ACTIVITY] Log yazılamadı — {}: {}", endpoint, e.getMessage());
        }
    }
}

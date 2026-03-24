package selahattin.dev.ecom.service.domain.admin;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.admin.AuditLogResponse;
import selahattin.dev.ecom.entity.audit.AdminAuditLogEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.repository.audit.AdminAuditLogRepository;
import selahattin.dev.ecom.service.domain.UserService;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AdminAuditLogRepository auditLogRepository;
    private final UserService userService;

    @Transactional
    public void log(String action, String entityType, UUID entityId, Map<String, Object> metadata) {
        UserEntity currentUser = userService.getCurrentUser();

        AdminAuditLogEntity entry = AdminAuditLogEntity.builder()
                .user(currentUser)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(metadata)
                .build();

        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(UUID userId, String entityType, String action, Pageable pageable) {
        return auditLogRepository
                .findWithFilters(userId, entityType, action, pageable)
                .map(this::mapToResponse);
    }

    private AuditLogResponse mapToResponse(AdminAuditLogEntity log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userEmail(log.getUser() != null ? log.getUser().getEmail() : null)
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .metadata(log.getMetadata())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

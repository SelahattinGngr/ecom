package selahattin.dev.ecom.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditLogResponse {

    private UUID id;
    private UUID userId;
    private String userEmail;
    private String action;
    private String entityType;
    private UUID entityId;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
}

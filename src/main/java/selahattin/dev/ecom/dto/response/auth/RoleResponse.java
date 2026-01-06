package selahattin.dev.ecom.dto.response.auth;

import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleResponse {
    private UUID id;
    private String name;
    private String description;
    private Boolean isSystem;
    private Set<PermissionResponse> permissions;
}
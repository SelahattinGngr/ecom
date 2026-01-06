package selahattin.dev.ecom.dto.request.auth;

import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequest {
    private String name;
    private String description;
    private Set<UUID> permissionIds;
}
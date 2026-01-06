package selahattin.dev.ecom.dto.response.auth;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionResponse {
    private UUID id;
    private String name;
    private String description;
}
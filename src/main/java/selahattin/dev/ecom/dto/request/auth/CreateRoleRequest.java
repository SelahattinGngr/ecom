package selahattin.dev.ecom.dto.request.auth;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleRequest {
    @NotBlank(message = "Rol adı boş olamaz")
    private String name;
    private String description;
    private Set<UUID> permissionIds;
}
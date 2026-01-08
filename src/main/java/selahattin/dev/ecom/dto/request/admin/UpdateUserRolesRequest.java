package selahattin.dev.ecom.dto.request.admin;

import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRolesRequest {
    @NotEmpty(message = "En az bir rol seçilmelidir.")
    private Set<UUID> roleIds;
}
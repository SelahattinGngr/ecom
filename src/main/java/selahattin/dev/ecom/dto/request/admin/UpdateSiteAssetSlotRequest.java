package selahattin.dev.ecom.dto.request.admin;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateSiteAssetSlotRequest {

    @NotNull(message = "Asset ID boş olamaz")
    private UUID assetId;
}

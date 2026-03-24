package selahattin.dev.ecom.dto.response.site;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetSlotResponse {
    private String url;
    private String mimeType;
}

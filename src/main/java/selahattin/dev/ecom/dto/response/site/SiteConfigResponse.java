package selahattin.dev.ecom.dto.response.site;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SiteConfigResponse {
    private Map<String, String> settings;
    private Map<String, AssetSlotResponse> assets;
}

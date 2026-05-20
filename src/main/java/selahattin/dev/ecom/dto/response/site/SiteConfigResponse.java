package selahattin.dev.ecom.dto.response.site;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteConfigResponse {
    private Map<String, String> settings;
    private Map<String, AssetSlotResponse> assets;
}

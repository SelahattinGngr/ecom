package selahattin.dev.ecom.dto.response.site;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSlotResponse {
    private String url;
    private String mimeType;
}

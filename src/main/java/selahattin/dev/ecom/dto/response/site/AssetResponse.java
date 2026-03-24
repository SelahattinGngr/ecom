package selahattin.dev.ecom.dto.response.site;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetResponse {
    private UUID id;
    private String url;
    private String objectKey;
    private String mimeType;
    private Long bytes;
    private OffsetDateTime createdAt;
}

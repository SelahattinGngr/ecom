package selahattin.dev.ecom.dto.response.product;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class ImageResponse {
    private UUID id;
    private String url;
    private Integer displayOrder;
    private Boolean isThumbnail;
}
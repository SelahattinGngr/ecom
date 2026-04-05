package selahattin.dev.ecom.dto.response.product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal basePrice;
    private String categoryName;
    private Integer categoryId;
    private String categorySlug;
    private List<VariantResponse> variants;
    private List<ImageResponse> images;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}

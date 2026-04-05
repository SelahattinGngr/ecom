package selahattin.dev.ecom.dto.response.product;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class VariantResponse {
    private UUID id;
    private String sku;
    private String size;
    private String color;
    private BigDecimal price;
    private Integer stockQuantity;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
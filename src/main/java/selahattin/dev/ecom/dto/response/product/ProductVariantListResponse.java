package selahattin.dev.ecom.dto.response.product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantListResponse {

    // --- Variant ---
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

    // --- Product (timestamp yok) ---
    private UUID productId;
    private String productName;
    private String productSlug;
    private Boolean isShowcase;
    private String categoryName;
    private Integer categoryId;
    private String categorySlug;
    private List<ImageResponse> images;
    private Long variantCount;
}

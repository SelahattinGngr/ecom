package selahattin.dev.ecom.dto.request.product;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import selahattin.dev.ecom.utils.enums.ProductStatus;

@Getter
@Setter
public class AdminProductFilterRequest {

    // --- Ortak filtreler ---
    private String query;
    private Integer categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String productSize;
    private String color;

    // --- Admin'e özel filtreler ---
    private ProductStatus status = ProductStatus.ACTIVE;
    private String sku; // Varyant SKU'suna göre ara
    private Integer stockMin; // Minimum stok miktarı
    private Integer stockMax; // Maximum stok miktarı
    private Boolean isVariantActive; // true: aktif varyantlı, false: pasif varyantlı ürünleri listele
}

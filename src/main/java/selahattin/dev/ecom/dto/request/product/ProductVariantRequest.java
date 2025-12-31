package selahattin.dev.ecom.dto.request.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductVariantRequest {
    @NotBlank(message = "SKU boş olamaz")
    private String sku;

    private String size; // Örn: XL, 42
    private String color; // Örn: Mavi, #0000FF

    @NotNull(message = "Fiyat boş olamaz")
    @Min(0)
    private BigDecimal price;

    @NotNull(message = "Stok adedi boş olamaz")
    @Min(0)
    private Integer stockQuantity;

    private Boolean isActive;
}
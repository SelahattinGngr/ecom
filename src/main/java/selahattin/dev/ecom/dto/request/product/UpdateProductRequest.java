package selahattin.dev.ecom.dto.request.product;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateProductRequest {
    private Integer categoryId;
    private String name;
    private String description;
    private BigDecimal basePrice;
    // Slug genelde güncellenmez ama gerekirse eklenebilir
}
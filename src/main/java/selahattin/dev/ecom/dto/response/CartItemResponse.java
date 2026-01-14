package selahattin.dev.ecom.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private UUID id; // CartItem ID (Silmek/Güncellemek için)
    private UUID productId;
    private UUID variantId;
    private String productName;
    private String productSlug;
    private String sku;
    private String size;
    private String color;
    private String imageUrl; // Ürünün ana resmi
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}
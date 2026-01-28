package selahattin.dev.ecom.dto.response.order;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderItemResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private String sku;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subTotal;
    // Snapshot'tan gelen renk/beden/resim bilgileri
    private Map<String, Object> attributes;
}
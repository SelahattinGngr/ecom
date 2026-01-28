package selahattin.dev.ecom.dto.response.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Getter
@Builder
public class OrderResponse {
    private UUID id;
    private String orderNumber; // UUID'den türetebiliriz veya ayrı alan olabilir. Şimdilik ID yeterli.
    private OffsetDateTime createdAt;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private int itemCount; // Kaç parça ürün var?
    private String firstItemName; // Örn: "iPhone 13 ve 2 diğer ürün" demek için
    // private String paymentStatus; // İleride eklenebilir
}
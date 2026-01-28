package selahattin.dev.ecom.dto.response.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import selahattin.dev.ecom.dto.response.CartItemResponse;

@Getter
@Builder
public class OrderSummaryResponse {
    private UUID orderId; // Preview'da null olabilir
    private List<CartItemResponse> items;
    private BigDecimal subTotal; // Ara Toplam
    private BigDecimal totalAmount; // Genel Toplam
}
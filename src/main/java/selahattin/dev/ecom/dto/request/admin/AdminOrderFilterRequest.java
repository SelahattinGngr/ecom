package selahattin.dev.ecom.dto.request.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Getter
@Setter
public class AdminOrderFilterRequest {

    private OrderStatus status;

    // Müşteri adı veya e-posta ile arama
    private String query;

    // Sipariş tutarı aralığı
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    // Tarih aralığı
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;

    // Kargo bilgisi
    private String cargoFirm;
}

package selahattin.dev.ecom.dto.response.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import selahattin.dev.ecom.utils.enums.RefundStatus;

@Getter
@Builder
public class AdminRefundResponse {

    private UUID id;
    private String providerRefundId;
    private BigDecimal amount;
    private RefundStatus status;
    private String reason;

    // Sipariş bilgisi
    private UUID orderId;
    private String orderNumber;

    // Müşteri bilgisi
    private UUID userId;
    private String customerName;
    private String customerEmail;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

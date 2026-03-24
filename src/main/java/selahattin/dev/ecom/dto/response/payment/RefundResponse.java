package selahattin.dev.ecom.dto.response.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import selahattin.dev.ecom.utils.enums.RefundStatus;

@Getter
@Builder
public class RefundResponse {

    private UUID id;
    private UUID orderId;
    private BigDecimal amount;
    private RefundStatus status;
    private String reason;
    private OffsetDateTime createdAt;
}

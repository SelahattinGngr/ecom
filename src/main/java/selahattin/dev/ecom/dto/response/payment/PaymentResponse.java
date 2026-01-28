package selahattin.dev.ecom.dto.response.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import selahattin.dev.ecom.utils.enums.PaymentProvider;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@Getter
@Builder
public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private PaymentProvider provider;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionId; // Provider'dan dönen ID (örn: iyzico paymentId)
    private String description;
    private OffsetDateTime createdAt;
}
package selahattin.dev.ecom.dto.response.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import selahattin.dev.ecom.utils.enums.PaymentProvider;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@Getter
@Builder
public class AdminPaymentResponse {
    private UUID id;
    private String transactionId;
    private PaymentProvider provider;
    private BigDecimal amount;
    private PaymentStatus status;
    private String description;

    // Order & User Info
    private UUID orderId;
    private String orderNumber;
    private String customerName;
    private String customerEmail;

    private OffsetDateTime createdAt;
}
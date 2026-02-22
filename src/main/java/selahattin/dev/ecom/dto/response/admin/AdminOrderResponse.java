package selahattin.dev.ecom.dto.response.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Getter
@Builder
public class AdminOrderResponse {
    private UUID id;
    private String orderNumber; // UUID'den türetilmiş kısa kod
    private OrderStatus status;
    private BigDecimal totalAmount;
    private int itemCount;

    // Kullanıcı Bilgileri (UserEntity'den)
    private UUID userId;
    private String customerName; // firstName + lastName
    private String customerEmail;

    // --- MANUEL KARGO TAKIP ALANLARI ---
    private String cargoFirm;
    private String trackingCode;
    private OffsetDateTime shippedAt;

    private OffsetDateTime createdAt;
}
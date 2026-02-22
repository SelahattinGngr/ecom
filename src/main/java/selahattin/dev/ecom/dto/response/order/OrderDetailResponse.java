package selahattin.dev.ecom.dto.response.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Getter
@Builder
public class OrderDetailResponse {
    private UUID id;
    private OffsetDateTime createdAt;
    private OrderStatus status;
    private BigDecimal totalAmount;

    // Adres Bilgileri (JSONB'den Map olarak dönüyoruz)
    private Map<String, Object> shippingAddress;
    private Map<String, Object> billingAddress;

    // Teslimat Bilgisi
    private String recipientName;
    private String recipientPhone;

    // --- MANUEL KARGO TAKIP ALANLARI ---
    private String cargoFirm;
    private String trackingCode;
    private OffsetDateTime shippedAt;

    // Ürünler
    private List<OrderItemResponse> items;

    // İade Bilgileri (Varsa)
    private String returnReason;
    private String returnTrackingNo;
}
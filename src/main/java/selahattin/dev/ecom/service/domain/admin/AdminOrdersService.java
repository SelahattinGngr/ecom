package selahattin.dev.ecom.service.domain.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.dto.infra.EmailMessageDto;
import selahattin.dev.ecom.dto.request.admin.ShipOrderRequest;
import selahattin.dev.ecom.dto.request.admin.UpdateOrderStatusRequest;
import selahattin.dev.ecom.dto.response.admin.AdminOrderResponse;
import selahattin.dev.ecom.dto.response.order.OrderDetailResponse;
import selahattin.dev.ecom.dto.response.order.OrderItemResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.order.OrderRepository;
import selahattin.dev.ecom.service.infra.RedisQueueService;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrdersService {

    private final OrderRepository orderRepository;
    private final RedisQueueService redisQueueService;

    // --- LIST ORDERS (Filtreli) ---
    public Page<AdminOrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<OrderEntity> orders;

        if (status != null) {
            orders = orderRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return orders.map(this::mapToAdminOrderResponse);
    }

    // --- GET ORDER DETAIL ---
    public OrderDetailResponse getOrderDetail(UUID id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return mapToOrderDetailResponse(order);
    }

    // --- UPDATE STATUS ---
    @Transactional
    public void updateOrderStatus(UUID id, UpdateOrderStatusRequest request) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.setStatus(request.getStatus());
        orderRepository.save(order);

        log.info("[ADMIN] Sipariş durumu güncellendi. Order ID: {}, Yeni Durum: {}", order.getId(), order.getStatus());
    }

    // --- SHIP ORDER ---
    @Transactional
    public void shipOrder(UUID id, ShipOrderRequest request) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.RETURNED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "İptal edilmiş veya iade edilmiş sipariş kargolanamaz.");
        }

        order.setCargoFirm(request.getCargoFirm());
        order.setTrackingCode(request.getTrackingCode());
        order.setShippedAt(OffsetDateTime.now());
        order.setStatus(OrderStatus.SHIPPED);

        orderRepository.save(order);

        log.info("[ADMIN] Sipariş kargolandı. Order ID: {}, Firma: {}, Takip Kodu: {}", order.getId(),
                request.getCargoFirm(), request.getTrackingCode());
        // TODO: Atılan maillerde daha profesyonel bir template kullanılacak, firma
        // isimleri verilecek. Şimdilik basit bir metin mail atıyoruz.
        EmailMessageDto emailMessage = createEmailMessage(
                order.getUser().getEmail(),
                "Siparişiniz Kargolandı - Takip Bilgileri",
                String.format(
                        "Merhaba %s,%n%nSiparişiniz kargoya verildi!%n%nKargo Firması: %s%nTakip Kodu: %s%n%nTeşekkürler!",
                        order.getUser().getFirstName() != null ? order.getUser().getFirstName() : "Değerli Müşterimiz",
                        request.getCargoFirm(),
                        request.getTrackingCode()));

        redisQueueService.enqueueEmail(emailMessage);
    }

    // --- MAPPERS ---

    private AdminOrderResponse mapToAdminOrderResponse(OrderEntity order) {
        UserEntity user = order.getUser();
        String customerName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "");

        return AdminOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getId().toString().substring(0, 8).toUpperCase())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItems() != null ? order.getItems().size() : 0)
                .userId(user.getId())
                .customerName(customerName.trim())
                .customerEmail(user.getEmail())
                .cargoFirm(order.getCargoFirm())
                .trackingCode(order.getTrackingCode())
                .shippedAt(order.getShippedAt())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderDetailResponse mapToOrderDetailResponse(OrderEntity order) {
        List<OrderItemResponse> items = order.getItems().stream().map(item -> OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductVariant().getProduct().getId())
                .productName(item.getProductNameAtPurchase())
                .sku(item.getSkuAtPurchase())
                .unitPrice(item.getPriceAtPurchase())
                .quantity(item.getQuantity())
                .subTotal(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                .attributes(item.getVariantSnapshot())
                .build()).toList();

        return OrderDetailResponse.builder()
                .id(order.getId())
                .createdAt(order.getCreatedAt())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .recipientName(order.getShippingRecipientFirstName() + " " + order.getShippingRecipientLastName())
                .recipientPhone(order.getShippingRecipientPhoneNumber())
                .cargoFirm(order.getCargoFirm())
                .trackingCode(order.getTrackingCode())
                .shippedAt(order.getShippedAt())
                .items(items)
                .returnReason(order.getReturnReason())
                .returnTrackingNo(order.getReturnTrackingNo())
                .build();
    }

    private EmailMessageDto createEmailMessage(String to, String subject, String content) {
        return EmailMessageDto.builder()
                .to(to)
                .subject(subject)
                .content(content)
                .build();
    }
}
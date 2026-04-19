package selahattin.dev.ecom.service.domain.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.dto.infra.EmailMessageDto;
import selahattin.dev.ecom.dto.request.admin.AdminOrderFilterRequest;
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
import selahattin.dev.ecom.repository.order.spec.OrderSpecification;
import selahattin.dev.ecom.service.domain.PaymentService;
import selahattin.dev.ecom.service.infra.RedisQueueService;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrdersService {

    private final OrderRepository orderRepository;
    private final RedisQueueService redisQueueService;
    private final PaymentService paymentService;
    private final AuditLogService auditLogService;

    /**
     * H-07 — State Machine geçiş tablosu.
     *
     * Sadece iş akışında anlamlı olan geçişlere izin verilir.
     * RETURN_REQUESTED, RETURNED, CANCELLED terminal/özel durumlardır:
     *   - RETURN_REQUESTED → approveReturn() / rejectReturn() ile yönetilir.
     *   - RETURNED ve CANCELLED terminal durumlardır, geri dönüş olmaz.
     *
     * EnumSet, enum değerleri için HashSet'ten daha verimlidir (bit-vector tabanlı).
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING,   EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PAID,      EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, EnumSet.of(OrderStatus.SHIPPED,   OrderStatus.CANCELLED),
            OrderStatus.SHIPPED,   EnumSet.of(OrderStatus.DELIVERED)
    );

    public Page<AdminOrderResponse> getAllOrders(AdminOrderFilterRequest filter, Pageable pageable) {
        return orderRepository
                .findAll(OrderSpecification.withFilter(filter), pageable)
                .map(this::mapToAdminOrderResponse);
    }

    public OrderDetailResponse getOrderDetail(UUID id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return mapToOrderDetailResponse(order);
    }

    /**
     * Sipariş durumunu günceller.
     *
     * H-07: Geçiş, ALLOWED_TRANSITIONS state machine ile doğrulanır.
     * Geçersiz geçişler (örn. CANCELLED → PAID, SHIPPED → PENDING) reddedilir.
     * approveReturn / rejectReturn gibi özel iş akışları kendi metodlarını kullanır.
     */
    @Transactional
    public void updateOrderStatus(UUID id, UpdateOrderStatusRequest request) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus targetStatus = request.getStatus();

        Set<OrderStatus> allowedTargets = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowedTargets == null || !allowedTargets.contains(targetStatus)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Geçersiz durum geçişi: " + currentStatus + " → " + targetStatus +
                            ". Bu durumdaki sipariş için geçerli hedefler: " +
                            (allowedTargets != null ? allowedTargets : "yok (terminal durum)"));
        }

        order.setStatus(targetStatus);
        orderRepository.save(order);

        auditLogService.log("ORDER_STATUS_UPDATED", "ORDER", id,
                Map.of("previousStatus", currentStatus.name(), "newStatus", targetStatus.name()));

        log.info("[ADMIN] Sipariş durumu güncellendi. OrderId: {}, {} → {}", id, currentStatus, targetStatus);
    }

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

        log.info("[ADMIN] Sipariş kargolandı. OrderId: {}, Firma: {}, Takip: {}",
                id, request.getCargoFirm(), request.getTrackingCode());

        String customerName = order.getUser().getFirstName() != null
                ? order.getUser().getFirstName()
                : "Değerli Müşterimiz";

        redisQueueService.enqueueEmail(createEmailMessage(
                order.getUser().getEmail(),
                "Siparişiniz Kargolandı - Takip Bilgileri",
                String.format(
                        "Merhaba %s,%n%nSiparişiniz kargoya verildi!%n%n" +
                                "Kargo Firması: %s%nTakip Kodu: %s%n%nTeşekkürler!",
                        customerName, request.getCargoFirm(), request.getTrackingCode())));
    }

    /**
     * İade talebi onaylandı:
     * 1. Hangi provider ile ödeme yapıldıysa o provider üzerinden iade başlatılır.
     * 2. Sipariş durumu RETURNED yapılır.
     * 3. Kullanıcıya bilgi maili gider.
     */
    @Transactional
    public void approveReturn(UUID id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Sadece iade bekleyen siparişler onaylanabilir.");
        }

        paymentService.refundByOrderId(order.getId());

        order.setStatus(OrderStatus.RETURNED);
        orderRepository.save(order);

        auditLogService.log("RETURN_APPROVED", "ORDER", id,
                Map.of("returnCode", order.getReturnCode() != null ? order.getReturnCode() : ""));

        log.info("[ADMIN] İade onaylandı. OrderId: {}", id);

        String customerName = order.getUser().getFirstName() != null
                ? order.getUser().getFirstName()
                : "Değerli Müşterimiz";

        redisQueueService.enqueueEmail(createEmailMessage(
                order.getUser().getEmail(),
                "İade Talebiniz Onaylandı",
                String.format(
                        "Merhaba %s,%n%nİade talebiniz onaylandı. " +
                                "Ödemeniz en kısa sürede hesabınıza aktarılacaktır.%n%n" +
                                "İade Kodu: %s%n%nTeşekkürler!",
                        customerName,
                        order.getReturnCode() != null ? order.getReturnCode() : "-")));
    }

    /**
     * İade talebi reddedildi:
     * Sipariş PAID durumuna geri döner, kullanıcıya bilgi maili gider.
     */
    @Transactional
    public void rejectReturn(UUID id, String reason) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Sadece iade bekleyen siparişler reddedilebilir.");
        }

        order.setStatus(OrderStatus.PAID);
        order.setReturnReason(null);
        order.setReturnedAt(null);
        order.setReturnCode(null);
        orderRepository.save(order);

        auditLogService.log("RETURN_REJECTED", "ORDER", id,
                Map.of("reason", reason != null ? reason : ""));

        log.info("[ADMIN] İade reddedildi. OrderId: {}, Neden: {}", id, reason);

        String customerName = order.getUser().getFirstName() != null
                ? order.getUser().getFirstName()
                : "Değerli Müşterimiz";

        redisQueueService.enqueueEmail(createEmailMessage(
                order.getUser().getEmail(),
                "İade Talebiniz Reddedildi",
                String.format(
                        "Merhaba %s,%n%nİade talebiniz incelendi ancak onaylanamadı.%n%n" +
                                "Red Nedeni: %s%n%nDetaylı bilgi için bizimle iletişime geçebilirsiniz.%n%nTeşekkürler!",
                        customerName, reason != null ? reason : "-")));
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
                .returnCode(order.getReturnCode())
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

package selahattin.dev.ecom.service.domain.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.admin.UpdateOrderStatusRequest;
import selahattin.dev.ecom.dto.response.admin.AdminOrderResponse;
import selahattin.dev.ecom.dto.response.order.OrderDetailResponse;
import selahattin.dev.ecom.dto.response.order.OrderItemResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.order.OrderRepository;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Service
@RequiredArgsConstructor
public class AdminOrdersService {

    private final OrderRepository orderRepository;

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

        // Opsiyonel: Geçiş kuralları eklenebilir (Örn: DELIVERED -> PENDING olamaz)
        // Şimdilik Admin God-Mode olduğu için serbest bırakıyoruz.

        order.setStatus(request.getStatus());
        orderRepository.save(order);

        // TODO: Durum değiştiğinde kullanıcıya mail at (Siparişiniz kargolandı vb.)
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
                .itemCount(order.getItems().size())
                .userId(user.getId())
                .customerName(customerName.trim())
                .customerEmail(user.getEmail())
                .createdAt(order.getCreatedAt())
                .build();
    }

    // [REFACTOR] Bu metodu OrderService'den kopyaladık veya ortak bir Mapper
    // class'ına taşıyabiliriz.
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
                .items(items)
                .returnReason(order.getReturnReason())
                .returnTrackingNo(order.getReturnTrackingNo())
                .build();
    }
}
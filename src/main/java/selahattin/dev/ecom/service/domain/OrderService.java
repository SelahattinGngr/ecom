package selahattin.dev.ecom.service.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.order.CheckoutRequest;
import selahattin.dev.ecom.dto.request.order.ReturnOrderRequest;
import selahattin.dev.ecom.dto.response.CartItemResponse;
import selahattin.dev.ecom.dto.response.CartResponse;
import selahattin.dev.ecom.dto.response.order.OrderDetailResponse;
import selahattin.dev.ecom.dto.response.order.OrderItemResponse;
import selahattin.dev.ecom.dto.response.order.OrderResponse;
import selahattin.dev.ecom.dto.response.order.OrderSummaryResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;
import selahattin.dev.ecom.entity.location.AddressEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.entity.order.OrderItemEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.catalog.ProductVariantRepository;
import selahattin.dev.ecom.repository.location.AddressRepository;
import selahattin.dev.ecom.repository.order.OrderRepository;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final UserService userService;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ObjectMapper objectMapper;

    // --- PREVIEW ---
    public OrderSummaryResponse checkoutPreview(CheckoutRequest request) {
        CartResponse cart = cartService.getMyCart();
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        return calculateOrderSummary(cart, null);
    }

    // --- CHECKOUT (CREATE ORDER) ---
    @Transactional
    public OrderSummaryResponse checkout(CheckoutRequest request) {
        UserEntity user = userService.getCurrentUser();
        CartResponse cart = cartService.getMyCart();

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        // Adresleri Getir ve Doğrula
        AddressEntity shippingAddress = addressRepository.findById(request.getShippingAddressId())
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND, "Teslimat adresi geçersiz"));

        AddressEntity billingAddress = addressRepository.findById(request.getBillingAddressId())
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND, "Fatura adresi geçersiz"));

        // Stok Kontrolü ve Düşümü
        for (CartItemResponse item : cart.getItems()) {
            ProductVariantEntity variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VARIANT_NOT_FOUND));

            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                        variant.getProduct().getName() + " için stok yetersiz.");
            }

            // Stoğu düş
            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            productVariantRepository.save(variant);
        }

        // Sipariş Oluştur
        OrderSummaryResponse summary = calculateOrderSummary(cart, null);

        OrderEntity order = OrderEntity.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(summary.getTotalAmount())
                .shippingRecipientFirstName(shippingAddress.getContactName().split(" ")[0])
                .shippingRecipientLastName(shippingAddress.getContactName().contains(" ")
                        ? shippingAddress.getContactName().substring(shippingAddress.getContactName().indexOf(" ") + 1)
                        : "")
                .shippingRecipientPhoneNumber(shippingAddress.getContactPhone())
                .shippingCountry(shippingAddress.getCountry())
                .shippingCity(shippingAddress.getCity())
                .shippingDistrict(shippingAddress.getDistrict())
                .shippingPostalCode(shippingAddress.getPostalCode())
                .shippingAddress(convertAddressToMap(shippingAddress))
                .billingAddress(convertAddressToMap(billingAddress))
                .items(new ArrayList<>())
                .build();

        // Order Itemları Oluştur (Snapshot mantığı)
        List<OrderItemEntity> orderItems = cart.getItems().stream().map(cartItem -> {
            Map<String, Object> snapshot = Map.of(
                    "color", cartItem.getColor() != null ? cartItem.getColor() : "",
                    "size", cartItem.getSize() != null ? cartItem.getSize() : "",
                    "imageUrl", cartItem.getImageUrl() != null ? cartItem.getImageUrl() : "");

            ProductVariantEntity variantRef = productVariantRepository.getReferenceById(cartItem.getVariantId());

            return OrderItemEntity.builder()
                    .order(order)
                    .productVariant(variantRef)
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(cartItem.getUnitPrice())
                    .productNameAtPurchase(cartItem.getProductName())
                    .skuAtPurchase(cartItem.getSku())
                    .variantSnapshot(snapshot)
                    .build();
        }).toList();

        order.setItems(orderItems);
        OrderEntity savedOrder = orderRepository.save(order);

        // Sepeti temizle
        cartService.clearCart();

        return OrderSummaryResponse.builder()
                .orderId(savedOrder.getId())
                .items(cart.getItems())
                .subTotal(summary.getSubTotal())
                .totalAmount(summary.getTotalAmount())
                .build();
    }

    // --- GET MY ORDERS (LIST) ---
    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        UserEntity user = userService.getCurrentUser();
        Page<OrderEntity> orders = orderRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return orders.map(this::mapToOrderResponse);
    }

    // --- GET ORDER DETAIL ---
    public OrderDetailResponse getOrderDetail(UUID orderId) {
        UserEntity user = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return mapToOrderDetailResponse(order);
    }

    // --- CANCEL ORDER ---
    @Transactional
    public void cancelOrder(UUID orderId) {
        UserEntity user = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PREPARING) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CANCELLED,
                    "Sadece beklemede veya hazırlanıyor aşamasındaki siparişler iptal edilebilir.");
        }

        order.setStatus(OrderStatus.CANCELLED);

        // Stokları geri yükle
        for (OrderItemEntity item : order.getItems()) {
            ProductVariantEntity variant = item.getProductVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            productVariantRepository.save(variant);
        }

        orderRepository.save(order);
    }

    // --- RETURN ORDER (IADE TALEBI) ---
    @Transactional
    public void createReturnRequest(UUID orderId, ReturnOrderRequest request) {
        UserEntity user = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Sadece teslim edilmiş siparişler iade edilebilir.");
        }

        if (order.getStatus() == OrderStatus.RETURNED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Bu sipariş zaten iade sürecinde.");
        }

        order.setReturnReason(request.getReason());
        order.setReturnedAt(OffsetDateTime.now());

        orderRepository.save(order);
    }

    // --- MAPPERS & HELPERS ---

    private OrderResponse mapToOrderResponse(OrderEntity order) {
        String firstItemName = order.getItems().isEmpty() ? "" : order.getItems().get(0).getProductNameAtPurchase();
        if (order.getItems().size() > 1) {
            firstItemName += " ve " + (order.getItems().size() - 1) + " ürün daha";
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getId().toString().substring(0, 8).toUpperCase())
                .createdAt(order.getCreatedAt())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItems().size())
                .firstItemName(firstItemName)
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
                .items(items)
                .returnReason(order.getReturnReason())
                .returnTrackingNo(order.getReturnTrackingNo())
                .build();
    }

    private OrderSummaryResponse calculateOrderSummary(CartResponse cart, String couponCode) {
        BigDecimal subTotal = cart.getTotalPrice();
        return OrderSummaryResponse.builder()
                .items(cart.getItems())
                .subTotal(subTotal)
                .totalAmount(subTotal)
                .build();
    }

    private Map<String, Object> convertAddressToMap(AddressEntity address) {
        try {
            return objectMapper.convertValue(address, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Adres verisi dönüştürülemedi");
        }
    }
}
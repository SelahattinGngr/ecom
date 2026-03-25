package selahattin.dev.ecom.service.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.infra.AddressSummaryDto;
import selahattin.dev.ecom.dto.infra.EmailMessageDto;
import selahattin.dev.ecom.dto.request.order.CheckoutPreviewRequest;
import selahattin.dev.ecom.dto.request.order.CheckoutRequest;
import selahattin.dev.ecom.dto.request.order.ReturnOrderRequest;
import selahattin.dev.ecom.dto.response.CartItemResponse;
import selahattin.dev.ecom.dto.response.CartResponse;
import selahattin.dev.ecom.dto.response.order.OrderDetailResponse;
import selahattin.dev.ecom.dto.response.order.OrderItemResponse;
import selahattin.dev.ecom.dto.response.order.OrderResponse;
import selahattin.dev.ecom.dto.response.order.OrderSummaryResponse;
import selahattin.dev.ecom.dto.response.payment.PaymentResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;
import selahattin.dev.ecom.entity.location.AddressEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.entity.order.OrderItemEntity;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.catalog.ProductVariantRepository;
import selahattin.dev.ecom.repository.location.AddressRepository;
import selahattin.dev.ecom.repository.order.OrderRepository;
import selahattin.dev.ecom.repository.payment.PaymentRepository;
import selahattin.dev.ecom.service.infra.RedisQueueService;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartService cartService;
    private final UserService userService;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ObjectMapper objectMapper;
    private final RedisQueueService redisQueueService;

    public OrderSummaryResponse checkoutPreview(CheckoutPreviewRequest request) {
        CartResponse cart = cartService.getMyCart();
        List<CartItemResponse> selectedItems = filterSelectedItems(cart, request.getItems());
        return calculateOrderSummary(selectedItems, null);
    }

    @Transactional
    public OrderSummaryResponse checkout(CheckoutRequest request) {
        UserEntity user = userService.getCurrentUser();
        CartResponse cart = cartService.getMyCart();

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        List<CartItemResponse> selectedItems = filterSelectedItems(cart, request.getItems());

        AddressEntity shippingAddress = addressRepository.findById(request.getShippingAddressId())
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        for (CartItemResponse item : selectedItems) {
            int updatedRows = productVariantRepository.decreaseStock(item.getVariantId(), item.getQuantity());
            if (updatedRows == 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, item.getProductName() + " stokta yok.");
            }
        }

        OrderSummaryResponse summary = calculateOrderSummary(selectedItems, null);

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
                .shippingAddress(convertAddressToMap(shippingAddress))
                .billingAddress(convertAddressToMap(addressRepository.findById(request.getBillingAddressId()).get()))
                .build();

        List<OrderItemEntity> orderItems = selectedItems.stream().map(cartItem -> OrderItemEntity.builder()
                .order(order)
                .productVariant(productVariantRepository.getReferenceById(cartItem.getVariantId()))
                .quantity(cartItem.getQuantity())
                .priceAtPurchase(cartItem.getUnitPrice())
                .productNameAtPurchase(cartItem.getProductName())
                .skuAtPurchase(cartItem.getSku())
                .variantSnapshot(Map.of("color", cartItem.getColor(), "size", cartItem.getSize()))
                .build()).toList();

        order.setItems(orderItems);
        orderRepository.save(order);

        selectedItems.forEach(item -> cartService.removeCartItem(item.getId()));

        return OrderSummaryResponse.builder()
                .orderId(order.getId())
                .totalAmount(order.getTotalAmount())
                .build();
    }

    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        UserEntity user = userService.getCurrentUser();
        Page<OrderEntity> orders = orderRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return orders.map(this::mapToOrderResponse);
    }

    public OrderDetailResponse getOrderDetail(UUID orderId) {
        UserEntity user = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return mapToOrderDetailResponse(order);
    }

    public PaymentResponse getOrderPayment(UUID orderId) {
        UserEntity user = userService.getCurrentUser();
        orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        PaymentEntity payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Bu siparişe ait ödeme kaydı bulunamadı"));

        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(orderId)
                .provider(payment.getPaymentProvider())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getPaymentTransactionId())
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        UserEntity user = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Bu sipariş zaten iptal edilmiş.");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PREPARING
                && order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CANCELLED,
                    "Sadece beklemede veya hazırlanıyor aşamasındaki siparişler iptal edilebilir.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        // TODO: Eğer ödeme yapıldıysa, ödeme iadesi işlemini başlat (bu örnekte ödeme
        // entegrasyonu olmadığı için atlanıyor)
        for (OrderItemEntity item : order.getItems()) {
            ProductVariantEntity variant = item.getProductVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            productVariantRepository.save(variant);
        }

        orderRepository.save(order);
    }

    @Transactional
    public void createReturnRequest(UUID orderId, ReturnOrderRequest request) {
        UserEntity user = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Sadece teslim edilmiş siparişler iade edilebilir.");
        }

        // İade takip kodu üret: "RET-" + orderId'nin ilk 8 karakteri büyük harf
        String returnCode = "RET-" + order.getId().toString().substring(0, 8).toUpperCase();

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnReason(request.getReason());
        order.setReturnedAt(OffsetDateTime.now());
        order.setReturnCode(returnCode);

        orderRepository.save(order);

        // Kullanıcıya iade bilgi maili gönder
        String customerName = user.getFirstName() != null ? user.getFirstName() : "Değerli Müşterimiz";
        EmailMessageDto email = EmailMessageDto.builder()
                .to(user.getEmail())
                .subject("İade Talebiniz Alındı - İade Kodunuz: " + returnCode)
                .content(String.format(
                        "Merhaba %s,%n%n" +
                                "İade talebiniz alınmıştır.%n%n" +
                                "İade Kodunuz: %s%n%n" +
                                "Bu kodu kargo paketinin üzerine yazmanızı rica ederiz. " +
                                "Paketiniz tarafımıza ulaştıktan sonra inceleme yapılacak " +
                                "ve iade işleminiz başlatılacaktır.%n%n" +
                                "İade Nedeniniz: %s%n%n" +
                                "Teşekkürler.",
                        customerName, returnCode, request.getReason()))
                .build();

        redisQueueService.enqueueEmail(email);
    }

    // --- MAPPERS & HELPERS ---

    private OrderResponse mapToOrderResponse(OrderEntity order) {
        String firstItemName = order.getItems().isEmpty() ? ""
                : order.getItems().get(0).getProductNameAtPurchase();
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
                .variantId(item.getProductVariant().getId())
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

    private OrderSummaryResponse calculateOrderSummary(List<CartItemResponse> items, String couponCode) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (CartItemResponse item : items) {
            subTotal = subTotal.add(item.getSubTotal());
        }
        return OrderSummaryResponse.builder()
                .items(items)
                .subTotal(subTotal)
                .totalAmount(subTotal)
                .build();
    }

    private Map<String, Object> convertAddressToMap(AddressEntity address) {
        try {
            AddressSummaryDto summaryDto = mapToAddressSummary(address);
            return objectMapper.convertValue(summaryDto, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Adres verisi dönüştürülemedi: " + e.getMessage());
        }
    }

    private AddressSummaryDto mapToAddressSummary(AddressEntity address) {
        return AddressSummaryDto.builder()
                .title(address.getTitle())
                .contactName(address.getContactName())
                .contactPhone(address.getContactPhone())
                .country(address.getCountry() != null ? address.getCountry().getName() : "")
                .city(address.getCity() != null ? address.getCity().getName() : "")
                .district(address.getDistrict() != null ? address.getDistrict().getName() : "")
                .neighborhood(address.getNeighborhood())
                .street(address.getStreet())
                .buildingNo(address.getBuildingNo())
                .apartmentNo(address.getApartmentNo())
                .postalCode(address.getPostalCode())
                .fullAddress(address.getFullAddress())
                .build();
    }

    private List<CartItemResponse> filterSelectedItems(CartResponse cart, List<UUID> selectedVariantIds) {
        if (selectedVariantIds == null || selectedVariantIds.isEmpty()) {
            throw new BusinessException(ErrorCode.CHECKOUT_EMPTY_CART, "En az bir ürün seçilmelidir.");
        }

        List<CartItemResponse> filtered = cart.getItems().stream()
                .filter(item -> selectedVariantIds.contains(item.getVariantId()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            throw new BusinessException(ErrorCode.CHECKOUT_EMPTY_CART, "Seçilen ürünler sepetinizde bulunamadı.");
        }

        return filtered;
    }
}
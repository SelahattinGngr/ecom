package selahattin.dev.ecom.service.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.order.CheckoutRequest;
import selahattin.dev.ecom.dto.response.CartItemResponse;
import selahattin.dev.ecom.dto.response.CartResponse;
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
    private final AddressRepository addressRepository; // [REFACTOR] Service yerine Repo kullanıldı refactor edilecek
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
        // CartService'de eklerken kontrol ediyoruz ama checkout anında tekrar etmek
        // şart.
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

        // Sipariş Oluştur (Hesaplamayı yap)
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

        // Order Itemları Oluştur
        List<OrderItemEntity> orderItems = cart.getItems().stream().map(cartItem -> {
            // Varyant detaylarını snapshot olarak sakla (isim, renk, beden, resim)
            Map<String, Object> snapshot = Map.of(
                    "color", cartItem.getColor() != null ? cartItem.getColor() : "",
                    "size", cartItem.getSize() != null ? cartItem.getSize() : "",
                    "imageUrl", cartItem.getImageUrl() != null ? cartItem.getImageUrl() : "");

            // Varyant referansını çek (Stock düşümü için yukarıda çekmiştik ama entity
            // referansı lazım)
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

        cartService.clearCart();

        return OrderSummaryResponse.builder()
                .orderId(savedOrder.getId())
                .items(cart.getItems())
                .subTotal(summary.getSubTotal())
                .totalAmount(summary.getTotalAmount())
                .build();
    }

    // --- HELPER: Hesaplama Motoru ---
    private OrderSummaryResponse calculateOrderSummary(CartResponse cart, String couponCode) {
        BigDecimal subTotal = cart.getTotalPrice();
        // simdilik sadece ürün fiyatları üzerinden hesaplama yapılıyor
        // ileride kargo, vergi, indirim vs eklenecek
        // satıcı kişi ürünleri fiyatlandırırken bunları dikkate alabilir
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
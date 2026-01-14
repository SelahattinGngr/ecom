package selahattin.dev.ecom.service.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.AddToCartRequest;
import selahattin.dev.ecom.dto.request.UpdateCartItemRequest;
import selahattin.dev.ecom.dto.response.CartItemResponse;
import selahattin.dev.ecom.dto.response.CartResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.catalog.ProductImageEntity;
import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;
import selahattin.dev.ecom.entity.order.CartEntity;
import selahattin.dev.ecom.entity.order.CartItemEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.catalog.ProductVariantRepository;
import selahattin.dev.ecom.repository.order.CartItemRepository;
import selahattin.dev.ecom.repository.order.CartRepository;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CART_CACHE_PREFIX = "cart:";
    private static final Duration CART_CACHE_TTL = Duration.ofDays(7);

    public CartResponse getMyCart() {
        UserEntity user = userService.getCurrentUser();
        String cacheKey = CART_CACHE_PREFIX + user.getId();

        CartResponse cachedCart = (CartResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cachedCart != null) {
            return cachedCart;
        }

        CartEntity cart = getOrCreateCart(user);

        CartResponse response = mapToResponse(cart);
        updateCartCache(user.getId(), response);

        return response;
    }

    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        UserEntity user = userService.getCurrentUser();
        CartEntity cart = getOrCreateCart(user);

        ProductVariantEntity variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIANT_NOT_FOUND));

        if (!variant.getIsActive()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE);
        }

        Optional<CartItemEntity> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductVariant().getId().equals(variant.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItemEntity item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();

            if (newQuantity > variant.getStockQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                        "Stok yetersiz. Mevcut stok: " + variant.getStockQuantity());
            }
            item.setQuantity(newQuantity);
        } else {
            if (request.getQuantity() > variant.getStockQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                        "Stok yetersiz. Mevcut stok: " + variant.getStockQuantity());
            }

            CartItemEntity newItem = CartItemEntity.builder()
                    .cart(cart)
                    .productVariant(variant)
                    .quantity(request.getQuantity())
                    .build();

            cart.getItems().add(newItem);
        }

        cartRepository.save(cart);

        CartResponse response = mapToResponse(cart);
        updateCartCache(user.getId(), response);
        return response;
    }

    @Transactional
    public CartResponse updateCartItem(UUID itemId, UpdateCartItemRequest request) {
        UserEntity user = userService.getCurrentUser();

        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (request.getQuantity() > item.getProductVariant().getStockQuantity()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    "Stok yetersiz. Max: " + item.getProductVariant().getStockQuantity());
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return refreshCart(user);
    }

    @Transactional
    public CartResponse removeCartItem(UUID itemId) {
        UserEntity user = userService.getCurrentUser();

        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        cartItemRepository.delete(item);

        return refreshCart(user);
    }

    @Transactional
    public void clearCart() {
        UserEntity user = userService.getCurrentUser();
        CartEntity cart = getOrCreateCart(user);

        cart.getItems().clear();
        cartRepository.save(cart);

        redisTemplate.delete(CART_CACHE_PREFIX + user.getId());
    }

    // --- HELPERS ---

    private CartEntity getOrCreateCart(UserEntity user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    CartEntity newCart = CartEntity.builder()
                            .user(user)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse refreshCart(UserEntity user) {
        CartEntity cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        CartResponse response = mapToResponse(cart);
        updateCartCache(user.getId(), response);
        return response;
    }

    private void updateCartCache(UUID userId, CartResponse cartResponse) {
        redisTemplate.opsForValue().set(
                CART_CACHE_PREFIX + userId,
                cartResponse,
                CART_CACHE_TTL);
    }

    private CartResponse mapToResponse(CartEntity cart) {
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalQuantity = 0;

        if (cart.getItems() != null) {
            for (CartItemEntity item : cart.getItems()) {
                ProductVariantEntity variant = item.getProductVariant();

                String imageUrl = null;
                if (variant.getProduct().getImages() != null && !variant.getProduct().getImages().isEmpty()) {

                    // isThumbnail olanı bulmaya çalış
                    imageUrl = variant.getProduct().getImages().stream()
                            .filter(img -> Boolean.TRUE.equals(img.getIsThumbnail()))
                            .findFirst()
                            .map(ProductImageEntity::getUrl)
                            .orElse(null);

                    // Eğer thumbnail yoksa, displayOrder'a göre en baştakini al
                    if (imageUrl == null) {
                        imageUrl = variant.getProduct().getImages().stream()
                                .sorted(Comparator.comparingInt(ProductImageEntity::getDisplayOrder))
                                .findFirst()
                                .map(ProductImageEntity::getUrl)
                                .orElse(null);
                    }
                }

                BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                totalPrice = totalPrice.add(lineTotal);
                totalQuantity += item.getQuantity();

                itemResponses.add(CartItemResponse.builder()
                        .id(item.getId())
                        .productId(variant.getProduct().getId())
                        .variantId(variant.getId())
                        .productName(variant.getProduct().getName())
                        .productSlug(variant.getProduct().getSlug())
                        .sku(variant.getSku())
                        .size(variant.getSize())
                        .color(variant.getColor())
                        .imageUrl(imageUrl)
                        .quantity(item.getQuantity())
                        .unitPrice(variant.getPrice())
                        .subTotal(lineTotal)
                        .build());
            }
        }

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalPrice(totalPrice)
                .totalQuantity(totalQuantity)
                .build();
    }
}
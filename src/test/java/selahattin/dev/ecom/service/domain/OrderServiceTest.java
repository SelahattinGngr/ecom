package selahattin.dev.ecom.service.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import selahattin.dev.ecom.dto.request.order.CheckoutRequest;
import selahattin.dev.ecom.dto.response.CartItemResponse;
import selahattin.dev.ecom.dto.response.CartResponse;
import selahattin.dev.ecom.dto.response.order.OrderSummaryResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.catalog.ProductEntity;
import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;
import selahattin.dev.ecom.entity.location.AddressEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.repository.catalog.ProductVariantRepository;
import selahattin.dev.ecom.repository.location.AddressRepository;
import selahattin.dev.ecom.repository.order.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartService cartService;
    @Mock
    private UserService userService;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private ObjectMapper objectMapper;
    // Unused in checkout test but present in service
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private OrderService orderService;

    @Test
    void checkout_ShouldCreateOrder_WhenStockIsSufficient() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail("test@test.com");

        UUID variantId = UUID.randomUUID();
        CartItemResponse cartItem = CartItemResponse.builder()
                .variantId(variantId)
                .quantity(2)
                .unitPrice(BigDecimal.TEN)
                .productName("Test Product")
                .build();

        CartResponse cart = CartResponse.builder()
                .items(List.of(cartItem))
                .totalPrice(BigDecimal.valueOf(20))
                .build();

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setId(variantId);
        variant.setStockQuantity(10);
        variant.setPrice(BigDecimal.TEN);
        variant.setProduct(new ProductEntity());

        UUID addressId = UUID.randomUUID();
        AddressEntity address = new AddressEntity();
        address.setId(addressId);
        address.setUser(user);
        address.setContactName("John Doe");

        CheckoutRequest request = new CheckoutRequest();
        request.setShippingAddressId(addressId);
        request.setBillingAddressId(addressId);

        when(userService.getCurrentUser()).thenReturn(user);
        when(cartService.getMyCart()).thenReturn(cart);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(productVariantRepository.getReferenceById(variantId)).thenReturn(variant);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> {
            OrderEntity order = i.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        // Act
        OrderSummaryResponse response = orderService.checkout(request);

        // Assert
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20));
        verify(productVariantRepository).save(variant); // Stock update
        assertThat(variant.getStockQuantity()).isEqualTo(8); // 10 - 2
        verify(cartService).clearCart();
    }

    @Test
    void checkout_ShouldThrowException_WhenStockIsInsufficient() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);

        UUID variantId = UUID.randomUUID();
        CartItemResponse cartItem = CartItemResponse.builder()
                .variantId(variantId)
                .quantity(5)
                .productName("Product")
                .build();

        CartResponse cart = CartResponse.builder().items(List.of(cartItem)).build();

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setId(variantId);
        variant.setStockQuantity(2); // Less than 5
        variant.setProduct(ProductEntity.builder().name("Product").build());

        AddressEntity address = new AddressEntity();
        address.setUser(user);

        CheckoutRequest request = new CheckoutRequest();
        request.setShippingAddressId(UUID.randomUUID());
        request.setBillingAddressId(UUID.randomUUID());

        when(userService.getCurrentUser()).thenReturn(user);
        when(cartService.getMyCart()).thenReturn(cart);
        when(addressRepository.findById(any())).thenReturn(Optional.of(address));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        // Act & Assert
        assertThatThrownBy(() -> orderService.checkout(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("stok yetersiz");
    }
}

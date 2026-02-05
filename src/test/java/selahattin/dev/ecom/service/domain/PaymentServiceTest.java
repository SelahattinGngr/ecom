package selahattin.dev.ecom.service.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.dto.response.payment.PaymentInitResponse;
import selahattin.dev.ecom.dto.response.payment.PaymentResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.order.OrderRepository;
import selahattin.dev.ecom.repository.payment.PaymentRepository;
import selahattin.dev.ecom.service.integration.payment.PaymentProviderStrategy;
import selahattin.dev.ecom.service.integration.payment.PaymentStrategyFactory;
import selahattin.dev.ecom.utils.enums.OrderStatus;
import selahattin.dev.ecom.utils.enums.PaymentProvider;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserService userService;

    @Mock
    private PaymentStrategyFactory paymentStrategyFactory;

    @Mock
    private PaymentProviderStrategy paymentProviderStrategy;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void initPayment_ShouldCreatePaymentAndReturnResponse_WhenOrderIsValid() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PaymentInitRequest request = new PaymentInitRequest();
        request.setOrderId(orderId);
        request.setProvider(PaymentProvider.STRIPE);

        UserEntity user = new UserEntity();
        user.setId(userId);

        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.TEN);

        PaymentEntity savedPayment = PaymentEntity.builder()
                .id(UUID.randomUUID())
                .order(order)
                .status(PaymentStatus.PENDING)
                .amount(BigDecimal.TEN)
                .paymentProvider(PaymentProvider.STRIPE)
                .build();

        PaymentInitResponse expectedResponse = PaymentInitResponse.builder()
                .paymentId(UUID.randomUUID())
                .redirectUrl("http://sandbox-api.iyzipay.com/payment")
                .build();

        when(userService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(savedPayment);
        when(paymentStrategyFactory.getStrategy(PaymentProvider.STRIPE)).thenReturn(paymentProviderStrategy);
        when(paymentProviderStrategy.initializePayment(savedPayment, request)) // Use specific object if eq() checks
                                                                               // fail, but here ref might differ. Pass
                                                                               // 'savedPayment' which is the mock
                                                                               // return.
                .thenReturn(expectedResponse);

        // Act
        PaymentInitResponse response = paymentService.initPayment(request);

        // Assert
        assertThat(response).isEqualTo(expectedResponse);
        verify(paymentRepository).save(any(PaymentEntity.class));
        verify(paymentProviderStrategy).initializePayment(savedPayment, request);
    }

    @Test
    void initPayment_ShouldThrowException_WhenOrderNotFound() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PaymentInitRequest request = new PaymentInitRequest();
        request.setOrderId(orderId);

        UserEntity user = new UserEntity();
        user.setId(userId);

        when(userService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.initPayment(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void initPayment_ShouldThrowException_WhenOrderNotPending() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PaymentInitRequest request = new PaymentInitRequest();
        request.setOrderId(orderId);

        UserEntity user = new UserEntity();
        user.setId(userId);

        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setUser(user);
        order.setStatus(OrderStatus.DELIVERED); // Not PENDING

        when(userService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.initPayment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bu sipariş ödeme için uygun değil.")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void getPaymentDetail_ShouldReturnDetail_WhenPaymentExistsAndBelongsToUser() {
        // Arrange
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserEntity user = new UserEntity();
        user.setId(userId);

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setId(UUID.randomUUID());

        PaymentEntity payment = PaymentEntity.builder()
                .id(paymentId)
                .order(order)
                .amount(BigDecimal.TEN)
                .paymentProvider(PaymentProvider.STRIPE)
                .status(PaymentStatus.SUCCEEDED)
                .build();

        when(userService.getCurrentUser()).thenReturn(user);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // Act
        PaymentResponse response = paymentService.getPaymentDetail(paymentId);

        // Assert
        assertThat(response.getId()).isEqualTo(paymentId);
        assertThat(response.getAmount()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void getPaymentDetail_ShouldThrowException_WhenPaymentNotFound() {
        // Arrange
        UUID paymentId = UUID.randomUUID();
        when(userService.getCurrentUser()).thenReturn(new UserEntity());
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.getPaymentDetail(paymentId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void getPaymentDetail_ShouldThrowException_WhenPaymentDoesNotBelongToUser() {
        // Arrange
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        UserEntity currentUser = new UserEntity();
        currentUser.setId(userId);

        UserEntity otherUser = new UserEntity();
        otherUser.setId(otherUserId);

        OrderEntity order = new OrderEntity();
        order.setUser(otherUser); // Order belongs to someone else

        PaymentEntity payment = PaymentEntity.builder()
                .id(paymentId)
                .order(order)
                .build();

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.getPaymentDetail(paymentId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bu ödemeyi görüntüleme yetkiniz yok")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}

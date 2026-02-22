package selahattin.dev.ecom.service.domain;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.config.properties.ClientProperties;
import selahattin.dev.ecom.config.properties.PaymentProperties;
import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.dto.response.payment.PaymentCallbackResult;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final PaymentProperties paymentProperties;
    private final ClientProperties clientProperties;

    @Transactional
    public PaymentInitResponse initPayment(PaymentInitRequest request) {
        UserEntity user = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUserId(request.getOrderId(), user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Bu sipariş ödeme için uygun değil.");
        }

        PaymentProvider activeProvider = paymentProperties.getActiveProvider();

        PaymentEntity payment = PaymentEntity.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .paymentProvider(activeProvider)
                .status(PaymentStatus.PENDING)
                .description("Sipariş ödemesi #" + order.getId())
                .build();

        PaymentEntity savedPayment = paymentRepository.save(payment);
        PaymentProviderStrategy strategy = paymentStrategyFactory.getStrategy(activeProvider);

        return strategy.initializePayment(savedPayment, request);
    }

    public PaymentResponse getPaymentDetail(UUID id) {
        UserEntity user = userService.getCurrentUser();

        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Ödeme bulunamadı"));

        if (!payment.getOrder().getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bu ödemeyi görüntüleme yetkiniz yok");
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .provider(payment.getPaymentProvider())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getPaymentTransactionId())
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    @Transactional
    public String processWebhook(PaymentProvider provider, Map<String, String> payload) {
        PaymentProviderStrategy strategy = paymentStrategyFactory.getStrategy(provider);
        PaymentCallbackResult result = strategy.processCallback(payload);

        String frontendUrl = clientProperties.getFrontendUrl();

        if (result.getTransactionId() == null) {
            log.error("[WEBHOOK] Transaction ID bulunamadı.");
            return frontendUrl + "/checkout/failure?error=invalid_payload";
        }

        // DİKKAT: PaymentRepository'ye findByPaymentTransactionId metodunu eklediğinden
        // emin ol.
        PaymentEntity payment = paymentRepository.findByPaymentTransactionId(result.getTransactionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Ödeme kaydı bulunamadı. Token: " + result.getTransactionId()));

        payment.setStatus(result.getStatus());

        if (result.getStatus() == PaymentStatus.SUCCEEDED) {
            payment.getOrder().setStatus(OrderStatus.PAID);
            paymentRepository.save(payment);
            orderRepository.save(payment.getOrder());
            return frontendUrl + "/checkout/success?orderId=" + payment.getOrder().getId();
        } else {
            payment.getOrder().setStatus(OrderStatus.CANCELLED);
            paymentRepository.save(payment);
            orderRepository.save(payment.getOrder());
            return frontendUrl + "/checkout/failure?orderId=" + payment.getOrder().getId() + "&errorCode="
                    + result.getErrorCode();
        }
    }
}
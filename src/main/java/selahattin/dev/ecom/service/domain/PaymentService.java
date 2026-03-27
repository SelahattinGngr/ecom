package selahattin.dev.ecom.service.domain;

import java.util.HashMap;
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
import selahattin.dev.ecom.entity.order.OrderItemEntity;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.catalog.ProductVariantRepository;
import selahattin.dev.ecom.repository.order.OrderRepository;
import selahattin.dev.ecom.repository.payment.PaymentRepository;
import selahattin.dev.ecom.service.integration.payment.PaymentProviderStrategy;
import selahattin.dev.ecom.service.integration.payment.PaymentStrategyFactory;
import selahattin.dev.ecom.utils.enums.OrderStatus;
import selahattin.dev.ecom.utils.enums.PaymentEventType;
import selahattin.dev.ecom.utils.enums.PaymentProvider;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserService userService;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final PaymentProperties paymentProperties;
    private final ClientProperties clientProperties;
    private final PaymentEventService paymentEventService;

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
                .clientIp(request.getClientIp())
                .build();

        PaymentEntity savedPayment = paymentRepository.save(payment);
        PaymentProviderStrategy strategy = paymentStrategyFactory.getStrategy(activeProvider);

        PaymentInitResponse response = strategy.initializePayment(savedPayment, request);

        paymentRepository.save(savedPayment);

        try {
            paymentEventService.log(savedPayment.getId(), PaymentEventType.PAYMENT_INITIATED, activeProvider,
                    Map.of("orderId", order.getId().toString(), "amount", order.getTotalAmount().toString()));
        } catch (Exception logEx) {
            log.warn("[PAYMENT] PAYMENT_INITIATED event yazılamadı — paymentId: {}", savedPayment.getId(), logEx);
        }

        return response;
    }

    public PaymentResponse getPaymentDetail(UUID id) {
        UserEntity user = userService.getCurrentUser();

        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Ödeme bulunamadı"));

        if (!payment.getOrder().getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bu ödemeyi görüntüleme yetkiniz yok");
        }

        return toPaymentResponse(payment);
    }

    @Transactional
    public String processWebhook(PaymentProvider provider, Map<String, String> payload) {
        PaymentProviderStrategy strategy = paymentStrategyFactory.getStrategy(provider);
        PaymentCallbackResult result = strategy.processCallback(payload);

        String frontendUrl = clientProperties.getFrontendUrl();

        if (result.getTransactionId() == null) {
            log.error("[WEBHOOK] Transaction ID bulunamadı. Provider: {}", provider);
            return frontendUrl + "/checkout/failure?error=invalid_payload";
        }

        PaymentEntity payment = paymentRepository.findByPaymentTransactionId(result.getTransactionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Ödeme kaydı bulunamadı. TransactionId: " + result.getTransactionId()));

        // Webhook'u aldık — ham payload'ı kaydet (itiraz/muhasebe için kanıt)
        try {
            Map<String, Object> rawPayload = new HashMap<>(payload);
            paymentEventService.log(payment.getId(), PaymentEventType.WEBHOOK_RECEIVED, provider, rawPayload);
        } catch (Exception logEx) {
            log.warn("[WEBHOOK] WEBHOOK_RECEIVED event yazılamadı — paymentId: {}", payment.getId(), logEx);
        }

        OrderEntity order = payment.getOrder();

        // IDEMPOTENCY KONTROLÜ: Eğer zaten işlenmişse direkt dön, sistemi yorma.
        if (payment.getStatus() == PaymentStatus.SUCCEEDED || order.getStatus() == OrderStatus.PAID) {
            log.info("[WEBHOOK] Idempotency: Bu işlem zaten başarılı olarak işaretlenmiş. OrderId: {}", order.getId());
            return frontendUrl + "/checkout/success?orderId=" + order.getId();
        }

        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.CANCELLED) {
            log.info("[WEBHOOK] Idempotency: Bu işlem zaten başarısız olarak işaretlenmiş. OrderId: {}", order.getId());
            return frontendUrl + "/checkout/failure?orderId=" + order.getId();
        }

        // Güncel provider verilerini kaydet
        payment.setStatus(result.getStatus());
        if (result.getProviderPaymentId() != null) {
            payment.setProviderPaymentId(result.getProviderPaymentId());
        }
        if (result.getItemTransactionIds() != null && !result.getItemTransactionIds().isEmpty()) {
            payment.setProviderItemTransactionIds(result.getItemTransactionIds());
        }

        if (result.getStatus() == PaymentStatus.SUCCEEDED) {
            order.setStatus(OrderStatus.PAID);
            paymentRepository.save(payment);
            orderRepository.save(order);

            try {
                paymentEventService.log(payment.getId(), PaymentEventType.PAYMENT_SUCCEEDED, provider,
                        Map.of("orderId", order.getId().toString(), "amount", payment.getAmount().toString()));
            } catch (Exception logEx) {
                log.warn("[WEBHOOK] PAYMENT_SUCCEEDED event yazılamadı — paymentId: {}", payment.getId(), logEx);
            }

            log.info("[WEBHOOK] Ödeme BAŞARILI. OrderId: {}", order.getId());
            return frontendUrl + "/checkout/success?orderId=" + order.getId();
        } else {
            order.setStatus(OrderStatus.CANCELLED);

            // H-05: Her ürün için SELECT + UPDATE yerine direkt UPDATE — N+1 önlemi.
            for (OrderItemEntity item : order.getItems()) {
                productVariantRepository.increaseStock(item.getProductVariant().getId(), item.getQuantity());
                log.info("[WEBHOOK] Başarısız ödeme, stok iade edildi. VariantID: {}, Miktar: {}",
                        item.getProductVariant().getId(), item.getQuantity());
            }

            paymentRepository.save(payment);
            orderRepository.save(order);

            try {
                paymentEventService.log(payment.getId(), PaymentEventType.PAYMENT_FAILED, provider,
                        Map.of("orderId", order.getId().toString(), "errorCode",
                                result.getErrorCode() != null ? result.getErrorCode() : ""));
            } catch (Exception logEx) {
                log.warn("[WEBHOOK] PAYMENT_FAILED event yazılamadı — paymentId: {}", payment.getId(), logEx);
            }

            log.warn("[WEBHOOK] Ödeme BAŞARISIZ. OrderId: {}, ErrorCode: {}", order.getId(), result.getErrorCode());
            return frontendUrl + "/checkout/failure?orderId=" + order.getId()
                    + "&errorCode=" + result.getErrorCode();
        }
    }

    /**
     * Siparişin başarılı ödemesini bulup hangi provider ile yapıldıysa
     * o provider üzerinden iade başlatır. Admin iade onayı buradan çağrılır.
     */
    @Transactional
    public void refundByOrderId(UUID orderId) {
        PaymentEntity payment = paymentRepository
                .findByOrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Bu sipariş için başarılı bir ödeme kaydı bulunamadı."));

        try {
            paymentEventService.log(payment.getId(), PaymentEventType.REFUND_INITIATED,
                    payment.getPaymentProvider(),
                    Map.of("orderId", orderId.toString(), "amount", payment.getAmount().toString()));
        } catch (Exception logEx) {
            log.warn("[REFUND] REFUND_INITIATED event yazılamadı — paymentId: {}", payment.getId(), logEx);
        }

        PaymentProviderStrategy strategy = paymentStrategyFactory.getStrategy(payment.getPaymentProvider());
        strategy.refundPayment(payment, payment.getAmount());

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        try {
            paymentEventService.log(payment.getId(), PaymentEventType.REFUND_COMPLETED,
                    payment.getPaymentProvider(),
                    Map.of("orderId", orderId.toString(), "amount", payment.getAmount().toString()));
        } catch (Exception logEx) {
            log.warn("[REFUND] REFUND_COMPLETED event yazılamadı — paymentId: {}", payment.getId(), logEx);
        }

        log.info("[REFUND] İade başarılı. OrderId: {}, Provider: {}, Tutar: {}",
                orderId, payment.getPaymentProvider(), payment.getAmount());
    }

    // --- MAPPER ---
    private PaymentResponse toPaymentResponse(PaymentEntity payment) {
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
}

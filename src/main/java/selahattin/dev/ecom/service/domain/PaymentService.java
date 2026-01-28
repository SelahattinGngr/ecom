package selahattin.dev.ecom.service.domain;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
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
import selahattin.dev.ecom.utils.enums.OrderStatus;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;

    @Value("${selahattin.dev.client.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public PaymentInitResponse initPayment(PaymentInitRequest request) {
        UserEntity user = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUserId(request.getOrderId(), user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Bu sipariş ödeme için uygun değil.");
        }

        PaymentEntity payment = PaymentEntity.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .paymentProvider(request.getProvider())
                .status(PaymentStatus.PENDING)
                .description("Sipariş ödemesi #" + order.getId())
                .build();

        paymentRepository.save(payment);

        // MOCK URL Üretimi
        // Kullanıcı bu linke tıkladığında Frontend'e gidecek, Frontend de
        // "Ödeme Başarılı" sayfası gösterecek.
        String mockRedirectUrl = frontendUrl + "/payment/mock-process?paymentId=" + payment.getId();

        return PaymentInitResponse.builder()
                .paymentId(payment.getId())
                .redirectUrl(mockRedirectUrl)
                .htmlContent("<p>Redirecting to mock payment provider...</p>")
                .build();
    }

    public PaymentResponse getPaymentDetail(UUID id) {
        UserEntity user = userService.getCurrentUser();

        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Ödeme bulunamadı"));

        // GÜVENLİK: Bu ödeme bu kullanıcıya mı ait?
        // Payment -> Order -> User ilişkisinden kontrol ediyoruz.
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
}
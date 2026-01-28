package selahattin.dev.ecom.service.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.dto.response.payment.PaymentInitResponse;
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

    @Value("${selahattin.dev.client.frontend-url}")
    private String frontendUrl;

    @Transactional
    public PaymentInitResponse initPayment(PaymentInitRequest request) {
        UserEntity user = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUserId(request.getOrderId(), user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Bu sipariş ödeme için uygun değil.");
        }

        // Ödeme Kaydı Oluştur
        PaymentEntity payment = PaymentEntity.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .paymentProvider(request.getProvider())
                .status(PaymentStatus.PENDING)
                .description("Sipariş ödemesi #" + order.getId())
                .build();

        paymentRepository.save(payment);

        String mockRedirectUrl = frontendUrl + "/payment/success?paymentId=" + payment.getId();

        return PaymentInitResponse.builder()
                .paymentId(payment.getId())
                .redirectUrl(mockRedirectUrl)
                .htmlContent("<html><body><h1>Mock Payment Redirect...</h1></body></html>")
                .build();
    }
}
package selahattin.dev.ecom.service.domain.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.admin.AdminPaymentResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.payment.PaymentRepository;
import selahattin.dev.ecom.service.integration.payment.PaymentProviderStrategy;
import selahattin.dev.ecom.service.integration.payment.PaymentStrategyFactory;
import selahattin.dev.ecom.utils.enums.OrderStatus;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@Service
@RequiredArgsConstructor
public class AdminPaymentsService {

    private final PaymentRepository paymentRepository;
    private final PaymentStrategyFactory paymentStrategyFactory;

    public Page<AdminPaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAllWithOrderAndUser(pageable)
                .map(this::mapToAdminResponse);
    }

    public AdminPaymentResponse getPaymentDetail(UUID id) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Ödeme bulunamadı"));
        return mapToAdminResponse(payment);
    }

    @Transactional
    public void capturePayment(UUID id) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.REQUIRES_ACTION
                && payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Bu ödeme zaten sonuçlanmış veya capture edilemez.");
        }

        PaymentProviderStrategy strategy = paymentStrategyFactory.getStrategy(payment.getPaymentProvider());
        strategy.capturePayment(payment);

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.getOrder().setStatus(OrderStatus.PAID);
        paymentRepository.save(payment);
    }

    @Transactional
    public void voidPayment(UUID id) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        PaymentProviderStrategy strategy = paymentStrategyFactory.getStrategy(payment.getPaymentProvider());
        strategy.voidPayment(payment);

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.getOrder().setStatus(OrderStatus.CANCELLED);
        paymentRepository.save(payment);
    }

    // --- MAPPER ---
    private AdminPaymentResponse mapToAdminResponse(PaymentEntity payment) {
        UserEntity user = payment.getOrder().getUser();
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "");

        return AdminPaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getPaymentTransactionId())
                .provider(payment.getPaymentProvider())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .description(payment.getDescription())
                .orderId(payment.getOrder().getId())
                .orderNumber(payment.getOrder().getId().toString().substring(0, 8).toUpperCase())
                .customerName(fullName.trim())
                .customerEmail(user.getEmail())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
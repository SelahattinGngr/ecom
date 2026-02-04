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
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@Service
@RequiredArgsConstructor
public class AdminPaymentsService {

    private final PaymentRepository paymentRepository;

    // --- LIST ---
    public Page<AdminPaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToAdminResponse);
    }

    // --- DETAIL ---
    public AdminPaymentResponse getPaymentDetail(UUID id) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Ödeme bulunamadı"));
        return mapToAdminResponse(payment);
    }

    // --- CAPTURE (Manuel Tahsilat) ---
    @Transactional
    public void capturePayment(UUID id) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.REQUIRES_ACTION && payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Bu ödeme zaten sonuçlanmış veya capture edilemez.");
        }

        // TODO: Provider (Iyzico/Stripe) servisine git ve parayı çek (Capture)

        payment.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);
    }

    // --- VOID (İptal - Gün sonu öncesi) ---
    @Transactional
    public void voidPayment(UUID id) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        // TODO: Provider servisine git ve işlemi iptal et (Void)

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);
    }

    // --- MAPPER ---
    private AdminPaymentResponse mapToAdminResponse(PaymentEntity payment) {
        UserEntity user = payment.getOrder().getUser();
        String fullName = user.getFirstName() + " " + user.getLastName();

        return AdminPaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getPaymentTransactionId())
                .provider(payment.getPaymentProvider())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .description(payment.getDescription())
                .orderId(payment.getOrder().getId())
                .orderNumber(payment.getOrder().getId().toString().substring(0, 8).toUpperCase())
                .customerName(fullName)
                .customerEmail(user.getEmail())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
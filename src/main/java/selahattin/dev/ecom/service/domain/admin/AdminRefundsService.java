package selahattin.dev.ecom.service.domain.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.admin.AdminRefundResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.entity.payment.RefundEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.payment.RefundRepository;
import selahattin.dev.ecom.utils.enums.RefundStatus;

@Service
@RequiredArgsConstructor
public class AdminRefundsService {

    private final RefundRepository refundRepository;

    public Page<AdminRefundResponse> getAllRefunds(Pageable pageable) {
        return refundRepository.findAllOrderByCreatedAtDesc(pageable)
                .map(this::mapToAdminResponse);
    }

    @Transactional
    public AdminRefundResponse updateRefundStatus(UUID id, RefundStatus status) {
        RefundEntity refund = refundRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "İade bulunamadı"));

        refund.setStatus(status);
        refundRepository.save(refund);

        return mapToAdminResponse(refund);
    }

    // --- MAPPER ---
    private AdminRefundResponse mapToAdminResponse(RefundEntity refund) {
        OrderEntity order = refund.getPayment().getOrder();
        UserEntity user = order.getUser();

        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();

        return AdminRefundResponse.builder()
                .id(refund.getId())
                .providerRefundId(refund.getProviderRefundId())
                .amount(refund.getAmount())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .orderId(order.getId())
                .orderNumber(order.getId().toString().substring(0, 8).toUpperCase())
                .userId(user.getId())
                .customerName(fullName)
                .customerEmail(user.getEmail())
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt())
                .build();
    }
}

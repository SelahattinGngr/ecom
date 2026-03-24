package selahattin.dev.ecom.service.domain;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.payment.RefundResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.payment.RefundEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.payment.RefundRepository;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final UserService userService;

    public Page<RefundResponse> getMyRefunds(Pageable pageable) {
        UserEntity user = userService.getCurrentUser();
        return refundRepository.findAllByUserId(user.getId(), pageable)
                .map(this::mapToResponse);
    }

    public RefundResponse getMyRefundDetail(UUID id) {
        UserEntity user = userService.getCurrentUser();
        RefundEntity refund = refundRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "İade bulunamadı"));
        return mapToResponse(refund);
    }

    private RefundResponse mapToResponse(RefundEntity refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .orderId(refund.getPayment().getOrder().getId())
                .amount(refund.getAmount())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .createdAt(refund.getCreatedAt())
                .build();
    }
}

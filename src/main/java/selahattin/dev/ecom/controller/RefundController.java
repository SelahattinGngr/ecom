package selahattin.dev.ecom.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.payment.RefundResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.RefundService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/refunds")
public class RefundController {

    private final RefundService refundService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RefundResponse>>> getMyRefunds(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "İadeler listelendi",
                refundService.getMyRefunds(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RefundResponse>> getMyRefundDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "İade detayı getirildi",
                refundService.getMyRefundDetail(id)));
    }
}

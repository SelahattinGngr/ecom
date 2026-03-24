package selahattin.dev.ecom.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.admin.UpdateRefundStatusRequest;
import selahattin.dev.ecom.dto.response.admin.AdminRefundResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.admin.AdminRefundsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/refunds")
public class AdminRefundsController {

    private final AdminRefundsService adminRefundsService;

    @GetMapping
    @PreAuthorize("hasAuthority('refund:read')")
    public ResponseEntity<ApiResponse<Page<AdminRefundResponse>>> getAllRefunds(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "İade listesi getirildi",
                adminRefundsService.getAllRefunds(pageable)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('refund:manage')")
    public ResponseEntity<ApiResponse<AdminRefundResponse>> updateRefundStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRefundStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "İade durumu güncellendi",
                adminRefundsService.updateRefundStatus(id, request.getStatus())));
    }
}

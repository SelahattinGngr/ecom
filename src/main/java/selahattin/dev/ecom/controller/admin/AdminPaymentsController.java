package selahattin.dev.ecom.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.admin.AdminPaymentResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.admin.AdminPaymentsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/payments")
public class AdminPaymentsController {

    private final AdminPaymentsService adminPaymentsService;

    @GetMapping
    @PreAuthorize("hasAuthority('payment:read')")
    public ResponseEntity<ApiResponse<Page<AdminPaymentResponse>>> getAllPayments(
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                "Ödemeler listelendi",
                adminPaymentsService.getAllPayments(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('payment:read')")
    public ResponseEntity<ApiResponse<AdminPaymentResponse>> getPaymentDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ödeme detayı getirildi",
                adminPaymentsService.getPaymentDetail(id)));
    }

    @PostMapping("/{id}/capture")
    @PreAuthorize("hasAuthority('payment:manage')")
    public ResponseEntity<ApiResponse<Void>> capturePayment(@PathVariable UUID id) {
        adminPaymentsService.capturePayment(id);
        return ResponseEntity.ok(ApiResponse.success("Ödeme tahsil edildi (Capture)"));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('payment:manage')")
    public ResponseEntity<ApiResponse<Void>> voidPayment(@PathVariable UUID id) {
        adminPaymentsService.voidPayment(id);
        return ResponseEntity.ok(ApiResponse.success("Ödeme iptal edildi (Void)"));
    }
}
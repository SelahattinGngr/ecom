package selahattin.dev.ecom.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.dto.response.payment.PaymentInitResponse;
import selahattin.dev.ecom.dto.response.payment.PaymentResponse;
import selahattin.dev.ecom.service.domain.PaymentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // Ödeme Başlat
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentInitResponse>> initPayment(
            @Valid @RequestBody PaymentInitRequest request,
            HttpServletRequest httpRequest) {
        request.setClientIp(extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(
                "Ödeme başlatıldı",
                paymentService.initPayment(request)));
    }

    // Ödeme Detayı Getir
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ödeme detayı getirildi",
                paymentService.getPaymentDetail(id)));
    }

    /**
     * Gerçek istemci IP'sini belirler.
     * Reverse proxy veya Next.js frontend arkasında çalışırken X-Forwarded-For
     * başlığı zincirinin ilk (en soldaki) değerini kullanır; başlık yoksa
     * doğrudan bağlantı adresine döner.
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // "client, proxy1, proxy2" biçimindeki değerden yalnızca ilk IP alınır
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}

package selahattin.dev.ecom.service.integration.payment.impl;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.dto.response.payment.PaymentCallbackResult;
import selahattin.dev.ecom.dto.response.payment.PaymentInitResponse;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.service.integration.payment.PaymentProviderStrategy;
import selahattin.dev.ecom.utils.enums.PaymentProvider;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@Slf4j
@Service
public class MockPaymentProvider implements PaymentProviderStrategy {

    @Value("${selahattin.dev.client.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public PaymentProvider getProviderName() {
        return PaymentProvider.MOCK;
    }

    @Override
    public PaymentInitResponse initializePayment(PaymentEntity payment, PaymentInitRequest request) {
        log.info("[MOCK] Ödeme başlatılıyor. Tutar: {}", payment.getAmount());

        String redirectUrl = frontendUrl + "/payment/mock-process?paymentId=" + payment.getId();

        return PaymentInitResponse.builder()
                .paymentId(payment.getId())
                .redirectUrl(redirectUrl)
                .htmlContent("<h1>Mock Payment Redirect</h1>")
                .build();
    }

    @Override
    public PaymentCallbackResult processCallback(Map<String, String> payload) {
        // Mock için callback payload'da "success" veya "fail" beklenir.
        // Frontend /payment/mock-process sayfasından yönlendirir.
        String result = payload.getOrDefault("result", "success");
        String paymentId = payload.getOrDefault("paymentId", null);

        PaymentStatus status = "success".equalsIgnoreCase(result)
                ? PaymentStatus.SUCCEEDED
                : PaymentStatus.FAILED;

        log.info("[MOCK] Callback alındı. PaymentId: {}, Sonuç: {}", paymentId, status);

        return PaymentCallbackResult.builder()
                .transactionId(paymentId)
                .status(status)
                .errorCode(status == PaymentStatus.FAILED ? "MOCK_FAILURE" : null)
                .build();
    }

    @Override
    public void capturePayment(PaymentEntity payment) {
        log.info("[MOCK] Ödeme TAHSİL EDİLDİ (Capture). ID: {}, Tutar: {}", payment.getId(), payment.getAmount());
    }

    @Override
    public void voidPayment(PaymentEntity payment) {
        log.info("[MOCK] Ödeme İPTAL EDİLDİ (Void). ID: {}", payment.getId());
    }

    @Override
    public void refundPayment(PaymentEntity payment, BigDecimal refundAmount) {
        log.info("[MOCK] Ödeme İADE EDİLDİ (Refund). ID: {}, Tutar: {}", payment.getId(), refundAmount);
    }
}
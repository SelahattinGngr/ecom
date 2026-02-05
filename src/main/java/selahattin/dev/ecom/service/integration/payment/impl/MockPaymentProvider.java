package selahattin.dev.ecom.service.integration.payment.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.dto.response.payment.PaymentInitResponse;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.service.integration.payment.PaymentProviderStrategy;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

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

        // Mock Redirect URL
        String redirectUrl = frontendUrl + "/payment/mock-process?paymentId=" + payment.getId();

        return PaymentInitResponse.builder()
                .paymentId(payment.getId())
                .redirectUrl(redirectUrl)
                .htmlContent("<h1>Mock Payment Redirect</h1>")
                .build();
    }

    @Override
    public void capturePayment(PaymentEntity payment) {
        log.info("[MOCK] Ödeme TAHSİL EDİLDİ (Capture). ID: {}, Tutar: {}", payment.getId(), payment.getAmount());
        // Gerçekte bankaya istek atılır, hata varsa Exception fırlatılır.
        // Mock olduğu için başarılı sayıyoruz.
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
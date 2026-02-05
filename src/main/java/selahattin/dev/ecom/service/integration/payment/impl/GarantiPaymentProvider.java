package selahattin.dev.ecom.service.integration.payment.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.dto.response.payment.PaymentInitResponse;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.service.integration.payment.PaymentProviderStrategy;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

@Slf4j
@Service
public class GarantiPaymentProvider implements PaymentProviderStrategy {

    @Override
    public PaymentProvider getProviderName() {
        return PaymentProvider.GARANTI;
    }

    @Override
    public PaymentInitResponse initializePayment(PaymentEntity payment, PaymentInitRequest request) {
        log.info("[GARANTI] Ödeme başlatma isteği geldi. Tutar: {}", payment.getAmount());

        // Buraya Garanti Sanal POS XML entegrasyonu gelecek.
        // Şimdilik hata fırlatıyoruz ki "Hazır değil" olduğu anlaşılsın.
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Garanti Bankası entegrasyonu henüz aktif değil.");
    }

    @Override
    public void capturePayment(PaymentEntity payment) {
        log.info("[GARANTI] Capture işlemi");
    }

    @Override
    public void voidPayment(PaymentEntity payment) {
        log.info("[GARANTI] Void işlemi");
    }

    @Override
    public void refundPayment(PaymentEntity payment, BigDecimal refundAmount) {
        log.info("[GARANTI] Refund işlemi");
    }
}
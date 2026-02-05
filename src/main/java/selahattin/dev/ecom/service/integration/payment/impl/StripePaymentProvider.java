package selahattin.dev.ecom.service.integration.payment.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.config.properties.PaymentProperties;
import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.dto.response.payment.PaymentInitResponse;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.service.integration.payment.PaymentProviderStrategy;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentProvider implements PaymentProviderStrategy {

    private final PaymentProperties paymentProperties;

    // Frontend URL'i ClientProperties'den de alabiliriz ama şimdilik burada
    // kalabilir
    // veya PaymentProperties'e taşıyabiliriz. Şimdilik properties'den devam.
    @Value("${selahattin.dev.client.frontend-url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        // Stripe API Key'i set et (Eğer Stripe seçiliyse)
        // Eğer null ise uygulama patlamasın, kullanırken patlasın.
        String apiKey = paymentProperties.getStripe().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            Stripe.apiKey = apiKey;
        }
    }

    @Override
    public PaymentProvider getProviderName() {
        return PaymentProvider.STRIPE;
    }

    @Override
    public PaymentInitResponse initializePayment(PaymentEntity payment, PaymentInitRequest request) {
        log.info("[STRIPE] Ödeme başlatılıyor. ID: {}", payment.getId());

        if (Stripe.apiKey == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Stripe API Key konfigüre edilmemiş!");
        }

        try {
            long amountInCents = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}&order_id="
                            + payment.getOrder().getId())
                    .setCancelUrl(frontendUrl + "/payment/cancel?order_id=" + payment.getOrder().getId())
                    .setClientReferenceId(payment.getId().toString())
                    .putMetadata("order_id", payment.getOrder().getId().toString())
                    .putMetadata("payment_id", payment.getId().toString())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("try")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Sipariş #" + payment.getOrder().getId())
                                                                    .setDescription("E-Ticaret Alışverişi")
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();

            Session session = Session.create(params);

            payment.setPaymentTransactionId(session.getId());

            return PaymentInitResponse.builder()
                    .paymentId(payment.getId())
                    .redirectUrl(session.getUrl())
                    .build();

        } catch (Exception e) {
            log.error("[STRIPE] Init Hatası: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.PAYMENT_INIT_ERROR, "Stripe hatası: " + e.getMessage());
        }
    }

    @Override
    public void capturePayment(PaymentEntity payment) {
        log.info("[STRIPE] Auto-Capture modunda çalışıyor. İşlem yapılmasına gerek yok. ID: {}", payment.getId());
    }

    @Override
    public void voidPayment(PaymentEntity payment) {
        log.info("[STRIPE] Void (İptal) işlemi başlatılıyor. Transaction ID: {}", payment.getPaymentTransactionId());

        try {
            Session session = Session.retrieve(payment.getPaymentTransactionId());
            String paymentIntentId = session.getPaymentIntent();

            if (paymentIntentId != null) {
                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                paymentIntent.cancel(PaymentIntentCancelParams.builder().build());
                log.info("[STRIPE] PaymentIntent iptal edildi: {}", paymentIntentId);
            } else {
                log.warn("[STRIPE] PaymentIntent bulunamadı veya session henüz tamamlanmamış.");
            }

        } catch (Exception e) {
            log.error("[STRIPE] Void Hatası: {}", e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, "Stripe iptal hatası: " + e.getMessage());
        }
    }

    @Override
    public void refundPayment(PaymentEntity payment, BigDecimal refundAmount) {
        log.info("[STRIPE] İade (Refund) işlemi. ID: {}, Tutar: {}", payment.getId(), refundAmount);

        try {
            Session session = Session.retrieve(payment.getPaymentTransactionId());
            String paymentIntentId = session.getPaymentIntent();

            if (paymentIntentId == null) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED, "İade edilecek bir PaymentIntent bulunamadı.");
            }

            long amountInCents = refundAmount.multiply(BigDecimal.valueOf(100)).longValue();

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(amountInCents)
                    .build();

            Refund refund = Refund.create(params);

            if (!"succeeded".equals(refund.getStatus()) && !"pending".equals(refund.getStatus())) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                        "Stripe iade durumu başarısız: " + refund.getStatus());
            }

            log.info("[STRIPE] İade başarılı. Refund ID: {}", refund.getId());

        } catch (Exception e) {
            log.error("[STRIPE] Refund Hatası: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, "Stripe iade hatası: " + e.getMessage());
        }
    }
}
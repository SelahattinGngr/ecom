package selahattin.dev.ecom.service.integration.payment;

import java.math.BigDecimal;
import java.util.Map;

import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.dto.response.payment.PaymentCallbackResult;
import selahattin.dev.ecom.dto.response.payment.PaymentInitResponse;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

public interface PaymentProviderStrategy {

    PaymentProvider getProviderName();

    PaymentInitResponse initializePayment(PaymentEntity payment, PaymentInitRequest request);

    /**
     * Webhook/Callback ile gelen veriyi işleyip sonucunu döner.
     */
    PaymentCallbackResult processCallback(Map<String, String> payload);

    void capturePayment(PaymentEntity payment);

    void voidPayment(PaymentEntity payment);

    void refundPayment(PaymentEntity payment, BigDecimal refundAmount);
}
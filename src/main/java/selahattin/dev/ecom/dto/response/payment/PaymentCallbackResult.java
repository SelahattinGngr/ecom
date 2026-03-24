package selahattin.dev.ecom.dto.response.payment;

import lombok.Builder;
import lombok.Getter;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@Getter
@Builder
public class PaymentCallbackResult {
    private String transactionId;
    private PaymentStatus status;
    private String errorCode;
    /** Iyzico'nun numeric paymentId'si — Cancel/Refund işlemleri için gerekli */
    private String providerPaymentId;
}
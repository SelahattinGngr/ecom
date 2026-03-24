package selahattin.dev.ecom.dto.response.payment;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@Getter
@Builder
public class PaymentCallbackResult {
    private String transactionId;
    private PaymentStatus status;
    private String errorCode;
    /** Iyzico'nun numeric paymentId'si — Cancel işlemi için gerekli */
    private String providerPaymentId;
    /** Iyzico'nun per-item paymentTransactionId listesi — Refund işlemi için gerekli */
    private List<String> itemTransactionIds;
}
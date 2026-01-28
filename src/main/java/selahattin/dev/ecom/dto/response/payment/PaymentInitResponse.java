package selahattin.dev.ecom.dto.response.payment;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentInitResponse {
    private UUID paymentId;
    private String redirectUrl; // Iyzico/Stripe ödeme sayfası linki
    private String htmlContent; // Iyzico bazen HTML döner
}
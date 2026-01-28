package selahattin.dev.ecom.dto.request.payment;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

@Getter
@Setter
public class PaymentInitRequest {
    @NotNull
    private UUID orderId;

    @NotNull
    private PaymentProvider provider;

    @NotNull
    private String callbackUrl; // Frontend'in ödeme bitince döneceği yer
}
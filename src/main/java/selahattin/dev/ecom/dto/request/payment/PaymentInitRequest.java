package selahattin.dev.ecom.dto.request.payment;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentInitRequest {
    @NotNull
    private UUID orderId;

    private String clientIp;
}
package selahattin.dev.ecom.dto.request.order;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {
    @NotNull(message = "Teslimat adresi seçilmelidir")
    private UUID shippingAddressId;

    @NotNull(message = "Fatura adresi seçilmelidir")
    private UUID billingAddressId;

    // private String couponCode; // İleride eklenecek
}
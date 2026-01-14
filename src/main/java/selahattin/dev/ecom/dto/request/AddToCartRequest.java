package selahattin.dev.ecom.dto.request;

import java.util.UUID;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddToCartRequest {
    @NotNull(message = "Varyant ID zorunludur")
    private UUID variantId;

    @Min(value = 1, message = "Adet en az 1 olmalıdır")
    private Integer quantity;
}
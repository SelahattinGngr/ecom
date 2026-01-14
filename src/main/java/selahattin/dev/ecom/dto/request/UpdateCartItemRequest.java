package selahattin.dev.ecom.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCartItemRequest {
    @NotNull(message = "Adet zorunludur")
    @Min(value = 1, message = "Adet en az 1 olmalıdır")
    private Integer quantity;
}
package selahattin.dev.ecom.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnOrderRequest {
    @NotBlank(message = "İade nedeni belirtilmelidir")
    private String reason;
}
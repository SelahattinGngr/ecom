package selahattin.dev.ecom.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipOrderRequest {

    @NotBlank(message = "Kargo firması boş bırakılamaz.")
    private String cargoFirm;

    @NotBlank(message = "Takip kodu boş bırakılamaz.")
    private String trackingCode;
}
package selahattin.dev.ecom.dto.request.order;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutPreviewRequest {

    @NotNull(message = "En az bir ürün seçilmelidir")
    private List<UUID> items;

}

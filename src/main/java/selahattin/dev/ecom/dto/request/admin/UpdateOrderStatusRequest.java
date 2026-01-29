package selahattin.dev.ecom.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Getter
@Setter
public class UpdateOrderStatusRequest {
    @NotNull(message = "Yeni durum (status) boş olamaz")
    private OrderStatus status;
}
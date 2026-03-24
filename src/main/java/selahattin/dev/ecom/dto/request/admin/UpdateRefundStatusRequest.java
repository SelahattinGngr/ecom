package selahattin.dev.ecom.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import selahattin.dev.ecom.utils.enums.RefundStatus;

@Getter
@NoArgsConstructor
public class UpdateRefundStatusRequest {

    @NotNull(message = "İade durumu boş olamaz")
    private RefundStatus status;
}

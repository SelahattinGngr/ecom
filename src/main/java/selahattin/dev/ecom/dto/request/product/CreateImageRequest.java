package selahattin.dev.ecom.dto.request.product;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateImageRequest {
    @NotBlank(message = "Görsel URL boş olamaz")
    private String url;

    private Integer displayOrder = 0;
    private Boolean isThumbnail = false;
}
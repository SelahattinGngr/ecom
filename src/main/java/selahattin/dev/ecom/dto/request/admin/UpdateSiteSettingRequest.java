package selahattin.dev.ecom.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateSiteSettingRequest {

    @NotBlank(message = "Değer boş olamaz")
    private String value;
}

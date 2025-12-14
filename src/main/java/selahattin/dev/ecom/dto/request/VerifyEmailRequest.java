package selahattin.dev.ecom.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailRequest {
    @NotBlank(message = "Token boş olamaz")
    private String token;
}
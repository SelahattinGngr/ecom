package selahattin.dev.ecom.dto.request;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Validated
public class VerifyOtpRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank(message = "Doğrulama kodu boş olamaz")
    private String code;
}
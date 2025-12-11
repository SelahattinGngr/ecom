package selahattin.dev.ecom.dto.request;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Validated
public class SigninWithPassword {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}

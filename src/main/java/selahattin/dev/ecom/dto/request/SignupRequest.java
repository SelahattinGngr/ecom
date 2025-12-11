package selahattin.dev.ecom.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupRequest {

    @Email(message = "Geçersiz Email Formatı")
    @NotBlank(message = "Email Boş Bırakılamaz")
    private String email;

    @NotBlank(message = "İsim Boş Bırakılamaz")
    private String firstName;

    @NotBlank(message = "Soyisim Boş Bırakılamaz")
    private String lastName;

}

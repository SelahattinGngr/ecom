package selahattin.dev.ecom.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {

    @Email(message = "Geçersiz Email Formatı")
    @NotBlank(message = "Email Boş Bırakılamaz")
    private String email;

    @NotBlank(message = "İsim Boş Bırakılamaz")
    @Size(max = 100, message = "İsim en fazla 100 karakter olabilir")
    private String firstName;

    @NotBlank(message = "Soyisim Boş Bırakılamaz")
    @Size(max = 100, message = "Soyisim en fazla 100 karakter olabilir")
    private String lastName;

}

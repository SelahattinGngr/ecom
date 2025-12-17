package selahattin.dev.ecom.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateAddressRequest {
    @NotBlank
    private String title; // Ev, İş

    @NotNull
    private Integer cityId;

    @NotNull
    private Integer districtId;

    @NotBlank
    private String neighborhood;

    @NotBlank
    private String fullAddress;

    @NotBlank
    private String contactName; // Kime teslim edilecek?

    @NotBlank
    @Pattern(regexp = "^\\+?[0 9]{10,15}$", message = "Geçersiz telefon formatı")
    private String contactPhone;
}
package selahattin.dev.ecom.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateAddressRequest {
    @NotBlank(message = "Adres başlığı boş olamaz")
    private String title; // Ev, İş

    @NotNull(message = "Ülke seçimi zorunludur")
    private Integer countryId;

    @NotNull(message = "Şehir seçimi zorunludur")
    private Integer cityId;

    @NotNull(message = "İlçe seçimi zorunludur")
    private Integer districtId;

    @NotBlank(message = "Posta kodu boş olamaz")
    private String zipCode;

    @NotBlank(message = "Mahalle boş olamaz")
    private String neighborhood;

    @NotBlank(message = "Açık adres boş olamaz")
    private String fullAddress;

    @NotBlank(message = "İletişim kurulacak kişi adı boş olamaz")
    private String contactName;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Geçersiz telefon formatı. Örn: 5551234567")
    private String contactPhone;
}
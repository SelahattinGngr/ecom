package selahattin.dev.ecom.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressResponse {
    private UUID id;
    private String title;
    private String cityName; // ID değil, isim dönüyoruz (Rize)
    private String districtName; // ID değil, isim dönüyoruz (Merkez)
    private String neighborhood;
    private String fullAddress;
    private String contactName;
    private String contactPhone;
}
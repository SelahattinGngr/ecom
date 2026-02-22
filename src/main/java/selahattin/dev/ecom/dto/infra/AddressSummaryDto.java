package selahattin.dev.ecom.dto.infra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressSummaryDto {
    private String title;
    private String contactName;
    private String contactPhone;

    // İlişkisel entityleri (City, District) sadece String veya basit obje olarak
    // tutuyoruz
    private String country;
    private String city;
    private String district;

    private String neighborhood;
    private String street;
    private String buildingNo;
    private String apartmentNo;
    private String postalCode;
    private String fullAddress;
}
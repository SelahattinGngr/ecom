package selahattin.dev.ecom.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CityResponse {
    private Integer id;
    private String name;
    // private Integer countryId; // Removed to avoid redundancy
}
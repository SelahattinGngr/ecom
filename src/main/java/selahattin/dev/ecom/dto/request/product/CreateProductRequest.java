package selahattin.dev.ecom.dto.request.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateProductRequest {
    @NotNull(message = "Kategori seçimi zorunludur")
    private Integer categoryId;

    @NotBlank(message = "Ürün adı boş olamaz")
    private String name;

    private String description;

    @NotNull(message = "Taban fiyat boş olamaz")
    @Min(value = 0, message = "Fiyat 0'dan küçük olamaz")
    private BigDecimal basePrice;
}
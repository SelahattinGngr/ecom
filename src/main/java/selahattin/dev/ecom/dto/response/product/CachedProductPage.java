package selahattin.dev.ecom.dto.response.product;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CachedProductPage {
    private List<ProductResponse> content;
    private long totalElements;
    private int pageNumber;
    private int pageSize;
}

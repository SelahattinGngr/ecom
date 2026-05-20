package selahattin.dev.ecom.dto.response.catalog;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Integer id;
    private String name;
    private String slug;
    private String imageUrl;
    private Integer parentId;
    private List<CategoryResponse> subCategories;
}
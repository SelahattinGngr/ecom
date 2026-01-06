package selahattin.dev.ecom.dto.request.category;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCategoryRequest {
    private String name;
    private Integer parentId;
    // Slug'ı manuel güncellemek isterlerse diye opsiyonel
    private String slug;
}
package selahattin.dev.ecom.dto.request.product;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductFilterRequest {
	String query;
	Integer categoryId;
	BigDecimal minPrice;
	BigDecimal maxPrice;
	String size;
	String color;
}

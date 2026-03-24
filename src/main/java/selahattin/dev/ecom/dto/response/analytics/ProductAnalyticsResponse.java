package selahattin.dev.ecom.dto.response.analytics;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductAnalyticsResponse {

    private Long totalItemsSold;
    private BigDecimal totalRevenue;
    private List<TopProductEntry> topProducts;

    @Getter
    @Builder
    public static class TopProductEntry {
        private String productName;
        private Long quantitySold;
        private BigDecimal revenue;
    }
}

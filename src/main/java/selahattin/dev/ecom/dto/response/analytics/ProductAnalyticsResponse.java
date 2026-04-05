package selahattin.dev.ecom.dto.response.analytics;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductAnalyticsResponse {

    private Kpis kpis;
    private Charts charts;

    @Getter
    @Builder
    public static class Kpis {
        private Long totalProducts;
        private Long activeProducts;
        private Long outOfStockCount;
        private Long lowStockCount;
    }

    @Getter
    @Builder
    public static class Charts {
        private List<TopSellingProductEntry> topSellingProducts;
        private List<CategoryRevenueEntry> categoryRevenue;
    }

    @Getter
    @Builder
    public static class TopSellingProductEntry {
        private String productId;
        private String name;
        private Long quantity;
    }

    @Getter
    @Builder
    public static class CategoryRevenueEntry {
        private String categoryId;
        private String name;
        private BigDecimal revenue;
    }
}

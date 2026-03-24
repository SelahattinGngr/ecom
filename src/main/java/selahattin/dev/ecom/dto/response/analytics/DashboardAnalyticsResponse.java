package selahattin.dev.ecom.dto.response.analytics;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardAnalyticsResponse {

    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long newCustomers;
    private BigDecimal averageOrderValue;
    private Map<String, Long> ordersByStatus;
    private List<DailyRevenueEntry> revenueChart;

    @Getter
    @Builder
    public static class DailyRevenueEntry {
        private String date;
        private BigDecimal revenue;
        private Long orders;
    }
}

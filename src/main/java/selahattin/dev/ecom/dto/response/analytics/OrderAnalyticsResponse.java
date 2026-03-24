package selahattin.dev.ecom.dto.response.analytics;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderAnalyticsResponse {

    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private Map<String, Long> byStatus;
    private List<DailyOrderEntry> dailyOrders;

    @Getter
    @Builder
    public static class DailyOrderEntry {
        private String date;
        private Long orders;
        private BigDecimal revenue;
    }
}

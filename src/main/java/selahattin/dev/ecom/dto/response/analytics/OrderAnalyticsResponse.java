package selahattin.dev.ecom.dto.response.analytics;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderAnalyticsResponse {

    private Kpis kpis;
    private Charts charts;

    @Getter
    @Builder
    public static class Kpis {
        private long todayOrders;
        private long pendingOrders;
        private double cancelledRate;
        private double averageShippingTimeHours;
        private long totalOrders;
        private java.math.BigDecimal totalRevenue;
        private java.math.BigDecimal averageOrderValue;
        private double returnRate;
    }

    @Getter
    @Builder
    public static class Charts {
        private List<DailyOrderTrendEntry> dailyOrderTrend;
        private List<StatusDistributionEntry> statusDistribution;
        private List<HourlyOrderEntry> hourlyOrderHeatmap;
    }

    @Getter
    @Builder
    public static class DailyOrderTrendEntry {
        private String date;
        private long orders;
    }

    @Getter
    @Builder
    public static class StatusDistributionEntry {
        private String status;
        private long count;
    }

    @Getter
    @Builder
    public static class HourlyOrderEntry {
        private String dayOfWeek;
        private int hour;
        private long orders;
    }
}

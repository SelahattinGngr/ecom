package selahattin.dev.ecom.dto.response.analytics;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnalyticsResponse {

    private Kpis kpis;
    private Charts charts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Kpis {

        private Long totalUsers;
        private Long newUsers;
        private Long activeUsers;
        private Double conversionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Charts {

        private List<DailyRegistrationEntry> dailyRegistrationTrend;
        private List<RoleDistributionEntry> roleDistribution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRegistrationEntry {

        private String date;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleDistributionEntry {

        private String role;
        private Long count;
    }
}

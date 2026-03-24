package selahattin.dev.ecom.dto.response.analytics;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserAnalyticsResponse {

    private Long totalUsers;
    private Long newUsers;
    private Long activeUsers;
    private List<DailyRegistrationEntry> dailyRegistrations;

    @Getter
    @Builder
    public static class DailyRegistrationEntry {
        private String date;
        private Long newUsers;
    }
}

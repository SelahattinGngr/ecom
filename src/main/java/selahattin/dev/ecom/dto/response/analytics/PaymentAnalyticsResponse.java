package selahattin.dev.ecom.dto.response.analytics;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentAnalyticsResponse {

    private Long totalPayments;
    private BigDecimal totalRevenue;
    private BigDecimal totalRefunded;
    private Map<String, Long> byStatus;
    private Map<String, Long> byProvider;
    private Map<String, BigDecimal> revenueByProvider;
}

package selahattin.dev.ecom.service.domain.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import selahattin.dev.ecom.dto.response.analytics.DashboardAnalyticsResponse;
import selahattin.dev.ecom.dto.response.analytics.OrderAnalyticsResponse;
import selahattin.dev.ecom.dto.response.analytics.PaymentAnalyticsResponse;
import selahattin.dev.ecom.dto.response.analytics.ProductAnalyticsResponse;
import selahattin.dev.ecom.dto.response.analytics.UserAnalyticsResponse;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.auth.UserRepository;
import selahattin.dev.ecom.repository.catalog.ProductRepository;
import selahattin.dev.ecom.repository.order.OrderItemRepository;
import selahattin.dev.ecom.repository.order.OrderRepository;
import selahattin.dev.ecom.repository.payment.PaymentRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    private void validateTimezone(String tz) {
        try {
            ZoneId.of(tz);
        } catch (DateTimeException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Geçersiz timezone: " + tz);
        }
    }

    public DashboardAnalyticsResponse getDashboardAnalytics(
        OffsetDateTime from,
        OffsetDateTime to,
        String tz
    ) {
        validateTimezone(tz);
        Long totalOrders = orderRepository.countByPeriod(from, to);
        BigDecimal totalRevenue = orderRepository.sumRevenueByPeriod(from, to);
        Long newCustomers = userRepository.countNewUsersByPeriod(from, to);
        BigDecimal avgOrderValue =
            totalOrders > 0
                ? totalRevenue.divide(
                      BigDecimal.valueOf(totalOrders),
                      2,
                      RoundingMode.HALF_UP
                  )
                : BigDecimal.ZERO;

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (Object[] row : orderRepository.countByStatusAndPeriod(from, to)) {
            ordersByStatus.put(
                row[0].toString(),
                ((Number) row[1]).longValue()
            );
        }

        List<DashboardAnalyticsResponse.DailyRevenueEntry> revenueChart =
            orderRepository
                .dailyOrderStats(from, to, tz)
                .stream()
                .map(row ->
                    DashboardAnalyticsResponse.DailyRevenueEntry.builder()
                        .date(row[0].toString())
                        .orders(((Number) row[1]).longValue())
                        .revenue(new BigDecimal(row[2].toString()))
                        .build()
                )
                .toList();

        return DashboardAnalyticsResponse.builder()
            .totalRevenue(totalRevenue)
            .totalOrders(totalOrders)
            .newCustomers(newCustomers)
            .averageOrderValue(avgOrderValue)
            .ordersByStatus(ordersByStatus)
            .revenueChart(revenueChart)
            .build();
    }

    public OrderAnalyticsResponse getOrderAnalytics(
        OffsetDateTime from,
        OffsetDateTime to,
        String tz
    ) {
        validateTimezone(tz);
        ZoneId zoneId = ZoneId.of(tz);
        ZonedDateTime todayStart = ZonedDateTime.now(zoneId).toLocalDate().atStartOfDay(zoneId);
        ZonedDateTime todayEnd = todayStart.plusDays(1);
        long todayOrders = orderRepository.countByPeriod(todayStart.toOffsetDateTime(), todayEnd.toOffsetDateTime());

        long totalOrders = orderRepository.countByPeriod(from, to);
        BigDecimal totalRevenue = orderRepository.sumRevenueByPeriod(from, to);
        BigDecimal avgOrderValue = totalOrders > 0
            ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        long pendingOrders = 0;
        long cancelledOrders = 0;
        long returnedOrders = 0;
        List<OrderAnalyticsResponse.StatusDistributionEntry> statusDistribution = new java.util.ArrayList<>();
        for (Object[] row : orderRepository.countByStatusAndPeriod(from, to)) {
            String status = row[0].toString();
            long count = ((Number) row[1]).longValue();
            statusDistribution.add(OrderAnalyticsResponse.StatusDistributionEntry.builder()
                .status(status)
                .count(count)
                .build());
            if ("PENDING".equals(status)) pendingOrders = count;
            if ("CANCELLED".equals(status)) cancelledOrders = count;
            if ("RETURNED".equals(status)) returnedOrders = count;
        }

        double cancelledRate = totalOrders > 0
            ? BigDecimal.valueOf((double) cancelledOrders / totalOrders)
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue()
            : 0.0;

        double returnRate = totalOrders > 0
            ? BigDecimal.valueOf((double) returnedOrders / totalOrders)
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue()
            : 0.0;

        Double avgShipping = orderRepository.averageShippingTimeHours(from, to);
        double averageShippingTimeHours = avgShipping != null
            ? BigDecimal.valueOf(avgShipping).setScale(1, RoundingMode.HALF_UP).doubleValue()
            : 0.0;

        List<OrderAnalyticsResponse.DailyOrderTrendEntry> dailyOrderTrend =
            orderRepository.dailyOrderStats(from, to, tz).stream()
                .map(row -> OrderAnalyticsResponse.DailyOrderTrendEntry.builder()
                    .date(row[0].toString())
                    .orders(((Number) row[1]).longValue())
                    .build())
                .toList();

        List<OrderAnalyticsResponse.HourlyOrderEntry> hourlyOrderHeatmap =
            orderRepository.hourlyOrderHeatmap(from, to, tz).stream()
                .map(row -> OrderAnalyticsResponse.HourlyOrderEntry.builder()
                    .dayOfWeek(row[0].toString().trim())
                    .hour(((Number) row[1]).intValue())
                    .orders(((Number) row[2]).longValue())
                    .build())
                .toList();

        return OrderAnalyticsResponse.builder()
            .kpis(OrderAnalyticsResponse.Kpis.builder()
                .todayOrders(todayOrders)
                .pendingOrders(pendingOrders)
                .cancelledRate(cancelledRate)
                .returnRate(returnRate)
                .averageShippingTimeHours(averageShippingTimeHours)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .averageOrderValue(avgOrderValue)
                .build())
            .charts(OrderAnalyticsResponse.Charts.builder()
                .dailyOrderTrend(dailyOrderTrend)
                .statusDistribution(statusDistribution)
                .hourlyOrderHeatmap(hourlyOrderHeatmap)
                .build())
            .build();
    }

    public PaymentAnalyticsResponse getPaymentAnalytics(
        OffsetDateTime from,
        OffsetDateTime to,
        String tz
    ) {
        Long totalPayments = paymentRepository.countByPeriod(from, to);
        BigDecimal totalRevenue = paymentRepository.sumRevenueByPeriod(
            from,
            to
        );
        BigDecimal totalRefunded = paymentRepository.sumRefundedByPeriod(
            from,
            to
        );

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : paymentRepository.countByStatusAndPeriod(
            from,
            to
        )) {
            byStatus.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        Map<String, Long> byProvider = new LinkedHashMap<>();
        Map<String, BigDecimal> revenueByProvider = new LinkedHashMap<>();
        for (Object[] row : paymentRepository.countAndRevenueByProviderAndPeriod(
            from,
            to
        )) {
            String provider = row[0].toString();
            byProvider.put(provider, ((Number) row[1]).longValue());
            revenueByProvider.put(provider, new BigDecimal(row[2].toString()));
        }

        return PaymentAnalyticsResponse.builder()
            .totalPayments(totalPayments)
            .totalRevenue(totalRevenue)
            .totalRefunded(totalRefunded)
            .byStatus(byStatus)
            .byProvider(byProvider)
            .revenueByProvider(revenueByProvider)
            .build();
    }

    public ProductAnalyticsResponse getProductAnalytics(
        OffsetDateTime from,
        OffsetDateTime to,
        String tz
    ) {
        List<ProductAnalyticsResponse.TopSellingProductEntry> topSellingProducts =
            orderItemRepository
                .findTopProductsByPeriod(from, to)
                .stream()
                .map(row ->
                    ProductAnalyticsResponse.TopSellingProductEntry.builder()
                        .productId(row[0] != null ? row[0].toString() : null)
                        .name(row[1] != null ? row[1].toString() : "Unknown")
                        .quantity(((Number) row[2]).longValue())
                        .build()
                )
                .toList();

        List<ProductAnalyticsResponse.CategoryRevenueEntry> categoryRevenue =
            orderItemRepository
                .findCategoryRevenueByPeriod(from, to)
                .stream()
                .map(row ->
                    ProductAnalyticsResponse.CategoryRevenueEntry.builder()
                        .categoryId(row[0] != null ? row[0].toString() : null)
                        .name(row[1] != null ? row[1].toString() : "Unknown")
                        .revenue(new BigDecimal(row[2].toString()))
                        .build()
                )
                .toList();

        return ProductAnalyticsResponse.builder()
            .kpis(
                ProductAnalyticsResponse.Kpis.builder()
                    .totalProducts(productRepository.countTotalProducts())
                    .activeProducts(productRepository.countActiveProducts())
                    .outOfStockCount(productRepository.countOutOfStockProducts())
                    .lowStockCount(productRepository.countLowStockProducts())
                    .build()
            )
            .charts(
                ProductAnalyticsResponse.Charts.builder()
                    .topSellingProducts(topSellingProducts)
                    .categoryRevenue(categoryRevenue)
                    .build()
            )
            .build();
    }

    public UserAnalyticsResponse getUserAnalytics(
        OffsetDateTime from,
        OffsetDateTime to,
        String tz
    ) {
        validateTimezone(tz);
        Long totalUsers = userRepository.countTotalActiveUsers();
        Long newUsers = userRepository.countNewUsersByPeriod(from, to);
        Long activeUsers = orderRepository.countDistinctCustomersByPeriod(
            from,
            to
        );

        double conversionRate =
            totalUsers > 0
                ? BigDecimal.valueOf((activeUsers * 100.0) / totalUsers)
                      .setScale(2, RoundingMode.HALF_UP)
                      .doubleValue()
                : 0.0;

        List<
            UserAnalyticsResponse.DailyRegistrationEntry
        > dailyRegistrationTrend = userRepository
            .dailyRegistrationStats(from, to, tz)
            .stream()
            .map(row ->
                UserAnalyticsResponse.DailyRegistrationEntry.builder()
                    .date(row[0].toString())
                    .count(((Number) row[1]).longValue())
                    .build()
            )
            .toList();

        List<UserAnalyticsResponse.RoleDistributionEntry> roleDistribution =
            userRepository
                .countUsersByRole()
                .stream()
                .map(row ->
                    UserAnalyticsResponse.RoleDistributionEntry.builder()
                        .role(row[0].toString())
                        .count(((Number) row[1]).longValue())
                        .build()
                )
                .toList();

        return UserAnalyticsResponse.builder()
            .kpis(
                UserAnalyticsResponse.Kpis.builder()
                    .totalUsers(totalUsers)
                    .newUsers(newUsers)
                    .activeUsers(activeUsers)
                    .conversionRate(conversionRate)
                    .build()
            )
            .charts(
                UserAnalyticsResponse.Charts.builder()
                    .dailyRegistrationTrend(dailyRegistrationTrend)
                    .roleDistribution(roleDistribution)
                    .build()
            )
            .build();
    }
}

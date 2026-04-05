package selahattin.dev.ecom.service.domain.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
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

    public DashboardAnalyticsResponse getDashboardAnalytics(
        OffsetDateTime from,
        OffsetDateTime to,
        String tz
    ) {
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
        Long totalOrders = orderRepository.countByPeriod(from, to);
        BigDecimal totalRevenue = orderRepository.sumRevenueByPeriod(from, to);
        BigDecimal avgOrderValue =
            totalOrders > 0
                ? totalRevenue.divide(
                      BigDecimal.valueOf(totalOrders),
                      2,
                      RoundingMode.HALF_UP
                  )
                : BigDecimal.ZERO;

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : orderRepository.countByStatusAndPeriod(from, to)) {
            byStatus.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        List<OrderAnalyticsResponse.DailyOrderEntry> dailyOrders =
            orderRepository
                .dailyOrderStats(from, to, tz)
                .stream()
                .map(row ->
                    OrderAnalyticsResponse.DailyOrderEntry.builder()
                        .date(row[0].toString())
                        .orders(((Number) row[1]).longValue())
                        .revenue(new BigDecimal(row[2].toString()))
                        .build()
                )
                .toList();

        return OrderAnalyticsResponse.builder()
            .totalOrders(totalOrders)
            .totalRevenue(totalRevenue)
            .averageOrderValue(avgOrderValue)
            .byStatus(byStatus)
            .dailyOrders(dailyOrders)
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

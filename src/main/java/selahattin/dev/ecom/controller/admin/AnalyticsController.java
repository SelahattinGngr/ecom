package selahattin.dev.ecom.controller.admin;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.analytics.DashboardAnalyticsResponse;
import selahattin.dev.ecom.dto.response.analytics.OrderAnalyticsResponse;
import selahattin.dev.ecom.dto.response.analytics.PaymentAnalyticsResponse;
import selahattin.dev.ecom.dto.response.analytics.ProductAnalyticsResponse;
import selahattin.dev.ecom.dto.response.analytics.UserAnalyticsResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.admin.AnalyticsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasAuthority('analytics:read')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardAnalyticsResponse>> getDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "UTC") String tz) {

        return ResponseEntity.ok(ApiResponse.success("OK", analyticsService.getDashboardAnalytics(from, to, tz)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<OrderAnalyticsResponse>> getOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "UTC") String tz) {

        return ResponseEntity.ok(ApiResponse.success("OK", analyticsService.getOrderAnalytics(from, to, tz)));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<PaymentAnalyticsResponse>> getPayments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "UTC") String tz) {

        return ResponseEntity.ok(ApiResponse.success("OK", analyticsService.getPaymentAnalytics(from, to, tz)));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<ProductAnalyticsResponse>> getProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "UTC") String tz) {

        return ResponseEntity.ok(ApiResponse.success("OK", analyticsService.getProductAnalytics(from, to, tz)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserAnalyticsResponse>> getUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "UTC") String tz) {

        return ResponseEntity.ok(ApiResponse.success("OK", analyticsService.getUserAnalytics(from, to, tz)));
    }
}

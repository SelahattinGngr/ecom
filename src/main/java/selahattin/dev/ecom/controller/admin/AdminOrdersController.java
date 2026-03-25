package selahattin.dev.ecom.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.admin.ShipOrderRequest;
import selahattin.dev.ecom.dto.request.admin.UpdateOrderStatusRequest;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.dto.response.admin.AdminOrderResponse;
import selahattin.dev.ecom.dto.response.order.OrderDetailResponse;
import selahattin.dev.ecom.service.domain.admin.AdminOrdersService;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/orders")
public class AdminOrdersController {

    private final AdminOrdersService adminOrdersService;

    @GetMapping
    @PreAuthorize("hasAuthority('order:read')")
    public ResponseEntity<ApiResponse<Page<AdminOrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                "Sipariş listesi getirildi",
                adminOrdersService.getAllOrders(status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:read')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sipariş detayı getirildi",
                adminOrdersService.getOrderDetail(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('order:update')")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        adminOrdersService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Sipariş durumu güncellendi"));
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAuthority('order:update')")
    public ResponseEntity<ApiResponse<Void>> shipOrder(
            @PathVariable UUID id,
            @Valid @RequestBody ShipOrderRequest request) {

        adminOrdersService.shipOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success("Sipariş başarıyla kargolandı."));
    }

    @PostMapping("/{id}/return/approve")
    @PreAuthorize("hasAuthority('order:update')")
    public ResponseEntity<ApiResponse<Void>> approveReturn(@PathVariable UUID id) {
        adminOrdersService.approveReturn(id);
        return ResponseEntity.ok(ApiResponse.success("İade onaylandı, ödeme iadesi başlatıldı."));
    }

    // FIXME: reason parametresi body içerisinde olsun
    @PostMapping("/{id}/return/reject")
    @PreAuthorize("hasAuthority('order:update')")
    public ResponseEntity<ApiResponse<Void>> rejectReturn(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {

        adminOrdersService.rejectReturn(id, reason);
        return ResponseEntity.ok(ApiResponse.success("İade talebi reddedildi."));
    }
}
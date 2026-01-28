package selahattin.dev.ecom.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.order.CheckoutRequest;
import selahattin.dev.ecom.dto.request.order.ReturnOrderRequest;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.dto.response.order.OrderDetailResponse;
import selahattin.dev.ecom.dto.response.order.OrderResponse;
import selahattin.dev.ecom.dto.response.order.OrderSummaryResponse;
import selahattin.dev.ecom.service.domain.OrderService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    // --- CHECKOUT ---
    @PostMapping("/checkout/preview")
    public ResponseEntity<ApiResponse<OrderSummaryResponse>> checkoutPreview(
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sipariş özeti oluşturuldu",
                orderService.checkoutPreview(request)));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderSummaryResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sipariş oluşturuldu",
                orderService.checkout(request)));
    }

    // --- MY ORDERS ---
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Siparişler listelendi",
                orderService.getMyOrders(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sipariş detayı getirildi",
                orderService.getOrderDetail(id)));
    }

    // --- ACTIONS ---
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Sipariş iptal edildi"));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<ApiResponse<Void>> returnOrder(
            @PathVariable UUID id,
            @Valid @RequestBody ReturnOrderRequest request) {
        orderService.createReturnRequest(id, request);
        return ResponseEntity.ok(ApiResponse.success("İade talebi oluşturuldu"));
    }
}
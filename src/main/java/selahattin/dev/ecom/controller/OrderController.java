package selahattin.dev.ecom.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.order.CheckoutRequest;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.dto.response.order.OrderSummaryResponse;
import selahattin.dev.ecom.service.domain.OrderService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout/preview")
    public ResponseEntity<selahattin.dev.ecom.response.ApiResponse<OrderSummaryResponse>> checkoutPreview(
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sipariş özeti oluşturuldu",
                orderService.checkoutPreview(request)));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderSummaryResponse>> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sipariş oluşturuldu",
                orderService.checkout(request)));
    }
}
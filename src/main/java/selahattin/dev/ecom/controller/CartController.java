package selahattin.dev.ecom.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.AddToCartRequest;
import selahattin.dev.ecom.dto.request.UpdateCartItemRequest;
import selahattin.dev.ecom.dto.response.CartResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.CartService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart() {
        return ResponseEntity.ok(ApiResponse.success(
                "Sepet getirildi",
                cartService.getMyCart()));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(@Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ürün sepete eklendi",
                cartService.addToCart(request)));
    }

    @PatchMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sepet güncellendi",
                cartService.updateCartItem(id, request)));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ürün sepetten silindi",
                cartService.removeCartItem(id)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok(ApiResponse.success("Sepet temizlendi"));
    }
}
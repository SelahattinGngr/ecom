package selahattin.dev.ecom.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.product.ProductFilterRequest;
import selahattin.dev.ecom.dto.response.product.ProductResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.ProductService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @ModelAttribute ProductFilterRequest filter,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 50) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                "Ürünler listelendi",
                productService.getProducts(filter, pageable)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ürün detayı getirildi",
                productService.getProductBySlug(slug)));
    }
}
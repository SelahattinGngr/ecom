package selahattin.dev.ecom.controller.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.product.AdminProductFilterRequest;
import selahattin.dev.ecom.dto.request.product.CreateProductRequest;
import selahattin.dev.ecom.dto.request.product.ProductVariantRequest;
import selahattin.dev.ecom.dto.request.product.UpdateProductRequest;
import selahattin.dev.ecom.dto.response.product.ProductResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.admin.AdminProductsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
public class AdminProductsController {

    private final AdminProductsService adminProductsService;

    // --- PRODUCT GET ---

    @GetMapping
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @ModelAttribute AdminProductFilterRequest filter,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ürünler listelendi",
                adminProductsService.getProducts(filter, pageable)));
    }

    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ürün detayı getirildi",
                adminProductsService.getProductBySlug(slug)));
    }

    // --- PRODUCT ---

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('product:create')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestPart("data") CreateProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        return ResponseEntity.ok(ApiResponse.success(
                "Ürün ve görseller oluşturuldu",
                adminProductsService.createProductWithImages(request, images)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('product:update')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ürün güncellendi",
                adminProductsService.updateProduct(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        adminProductsService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Ürün silindi (soft delete)"));
    }

    // --- VARIANTS ---

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasAuthority('product:update')") // Varyant eklemek güncelleme sayılır
    public ResponseEntity<ApiResponse<Void>> addVariant(
            @PathVariable UUID id,
            @Valid @RequestBody ProductVariantRequest request) {
        adminProductsService.addVariant(id, request);
        return ResponseEntity.ok(ApiResponse.success("Varyant eklendi"));
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasAuthority('product:update')")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        adminProductsService.deleteVariant(productId, variantId);
        return ResponseEntity.ok(ApiResponse.success("Varyant silindi"));
    }

    // --- IMAGES ---

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('product:update')")
    public ResponseEntity<ApiResponse<Void>> addImage(
            @PathVariable UUID id,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "isThumbnail", required = false) Boolean isThumbnail) {
        adminProductsService.addImage(id, image, displayOrder, isThumbnail);
        return ResponseEntity.ok(ApiResponse.success("Görsel eklendi"));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @PreAuthorize("hasAuthority('product:update')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable UUID productId,
            @PathVariable UUID imageId) {
        adminProductsService.deleteImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Görsel silindi"));
    }
}
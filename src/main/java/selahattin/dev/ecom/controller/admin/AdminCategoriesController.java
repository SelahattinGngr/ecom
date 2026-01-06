package selahattin.dev.ecom.controller.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import selahattin.dev.ecom.dto.request.category.CreateCategoryRequest;
import selahattin.dev.ecom.dto.request.category.UpdateCategoryRequest;
import selahattin.dev.ecom.dto.response.catalog.CategoryResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.admin.AdminCategoryService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoriesController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('category:manage')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                "Kategoriler listelendi",
                adminCategoryService.getAllCategories()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('category:manage')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Kategori oluşturuldu",
                adminCategoryService.createCategory(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('category:manage')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Integer id,
            @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Kategori güncellendi",
                adminCategoryService.updateCategory(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('category:manage')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Integer id) {
        adminCategoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Kategori silindi (Soft Delete)"));
    }
}
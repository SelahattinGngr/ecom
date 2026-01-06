package selahattin.dev.ecom.controller.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.auth.PermissionResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.admin.AdminPermissionService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/permissions")
public class AdminPermissionController {
    private final AdminPermissionService adminPermissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:manage')") // Sadece en üst yetkililer görebilir
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.success(
                "Yetkiler listelendi",
                adminPermissionService.getAllPermissions()));
    }
}
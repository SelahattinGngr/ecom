package selahattin.dev.ecom.controller.admin;

import java.util.List;
import java.util.UUID;

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
import selahattin.dev.ecom.dto.request.auth.CreateRoleRequest;
import selahattin.dev.ecom.dto.request.auth.UpdateRoleRequest;
import selahattin.dev.ecom.dto.response.auth.RoleResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.admin.AdminRoleService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/roles")
public class AdminRolesController {

    private final AdminRoleService adminRoleService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:manage')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(
                "Roller listelendi",
                adminRoleService.getAllRoles()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:manage')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Rol oluşturuldu",
                adminRoleService.createRole(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('system:manage')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID id,
            @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Rol güncellendi",
                adminRoleService.updateRole(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:manage')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        adminRoleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Rol silindi"));
    }
}
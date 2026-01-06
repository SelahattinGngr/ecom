package selahattin.dev.ecom.service.domain.admin;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.auth.CreateRoleRequest;
import selahattin.dev.ecom.dto.request.auth.UpdateRoleRequest;
import selahattin.dev.ecom.dto.response.auth.PermissionResponse;
import selahattin.dev.ecom.dto.response.auth.RoleResponse;
import selahattin.dev.ecom.entity.auth.PermissionEntity;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.exception.user.ResourceNotFoundException;
import selahattin.dev.ecom.repository.auth.PermissionRepository;
import selahattin.dev.ecom.repository.auth.RoleRepository;

@Service
@RequiredArgsConstructor
public class AdminRoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Bu isimde bir rol zaten mevcut.");
        }

        // Permission ID'lerini Entity Set'ine çevir
        Set<PermissionEntity> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissions.addAll(permissionRepository.findAllById(request.getPermissionIds()));
        }

        RoleEntity role = RoleEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isSystem(false) // API ile oluşturulan hiçbir rol SİSTEM rolü olamaz
                .permissions(permissions)
                .createdAt(OffsetDateTime.now())
                .build();

        return mapToResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse updateRole(UUID id, UpdateRoleRequest request) {
        RoleEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol bulunamadı"));

        // İsim değişikliğine sadece sistem olmayan rollerde izin verelim.
        if (StringUtils.hasText(request.getName())) {
            if (Boolean.TRUE.equals(role.getIsSystem()) && !role.getName().equals(request.getName())) {
                throw new IllegalArgumentException("Sistem rollerinin adı değiştirilemez.");
            }
            if (!role.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("Bu isimde başka bir rol zaten var.");
            }
            role.setName(request.getName());
        }

        if (StringUtils.hasText(request.getDescription())) {
            role.setDescription(request.getDescription());
        }

        if (request.getPermissionIds() != null) {
            Set<PermissionEntity> newPermissions = new HashSet<>(
                    permissionRepository.findAllById(request.getPermissionIds()));
            role.setPermissions(newPermissions);
        }

        return mapToResponse(roleRepository.save(role));
    }

    @Transactional
    public void deleteRole(UUID id) {
        RoleEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol bulunamadı"));

        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new IllegalArgumentException("Sistem rolleri (Admin, Developer, Customer) silinemez!");
        }

        // TODO: Bu role sahip kullanıcılar var mı kontrol edilecek

        // Önce bu role sahip kullanıcı var mı diye bakmak iyi olurdu ama
        // şu anlık direkt siliyoruz. İlişkisel veritabanında user_roles tablosundan da
        // düşmesi lazım (Cascade ayarı mevcut).
        roleRepository.delete(role);
    }

    private RoleResponse mapToResponse(RoleEntity entity) {
        Set<PermissionResponse> permissionResponses = (entity.getPermissions() == null)
                ? Collections.emptySet()
                : entity.getPermissions().stream()
                        .map(p -> PermissionResponse.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .description(p.getDescription())
                                .build())
                        .collect(Collectors.toSet());

        return RoleResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .isSystem(entity.getIsSystem())
                .permissions(permissionResponses)
                .build();
    }
}
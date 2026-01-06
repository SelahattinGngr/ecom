package selahattin.dev.ecom.service.domain.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.auth.PermissionResponse;
import selahattin.dev.ecom.entity.auth.PermissionEntity;
import selahattin.dev.ecom.repository.auth.PermissionRepository;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PermissionResponse mapToResponse(PermissionEntity entity) {
        return PermissionResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }
}
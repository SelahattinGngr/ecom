package selahattin.dev.ecom.service.domain.admin;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.admin.UpdateUserRolesRequest;
import selahattin.dev.ecom.dto.response.admin.AdminUserResponse;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.auth.RoleRepository;
import selahattin.dev.ecom.repository.auth.UserRepository;
import selahattin.dev.ecom.service.infra.TokenService;

@Service
@RequiredArgsConstructor
public class AdminUsersService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAllByDeletedAtIsNull(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(UUID id) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return mapToResponse(user);
    }

    @Transactional
    public AdminUserResponse updateUserRoles(UUID userId, UpdateUserRolesRequest request) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<RoleEntity> roles = roleRepository.findAllById(request.getRoleIds());

        if (roles.isEmpty()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Seçilen roller bulunamadı.");
        }

        if (roles.size() != request.getRoleIds().size()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Seçilen bazı roller sistemde bulunamadı.");
        }

        user.setRoles(new HashSet<>(roles));
        UserEntity savedUser = userRepository.save(user);

        // Yeni yetkilerini alabilmesi icin kullanıcının tüm oturumlarını kapat
        tokenService.deleteAllTokens(user.getEmail());

        return mapToResponse(savedUser);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String currentUserEmail = auth.getName();

        if (currentUserEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_OWN_USER);
        }

        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);

        tokenService.deleteAllTokens(user.getEmail());
    }

    private AdminUserResponse mapToResponse(UserEntity user) {
        List<String> roleNames = user.getRoles().stream()
                .map(RoleEntity::getName)
                .toList();

        return AdminUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .isEmailVerified(user.getEmailVerifiedAt() != null)
                .isPhoneVerified(user.getPhoneNumberVerifiedAt() != null)
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
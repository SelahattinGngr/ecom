package selahattin.dev.ecom.service.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.SignupRequest;
import selahattin.dev.ecom.dto.request.UpdateProfileRequest;
import selahattin.dev.ecom.dto.response.CurrentUserResponse;
import selahattin.dev.ecom.dto.response.SessionResponse;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.exception.auth.AuthenticationContextException;
import selahattin.dev.ecom.exception.user.ResourceNotFoundException;
import selahattin.dev.ecom.exception.user.UserAlreadyExistsException;
import selahattin.dev.ecom.repository.auth.RoleRepository;
import selahattin.dev.ecom.repository.auth.UserRepository;
import selahattin.dev.ecom.security.CustomUserDetails;
import selahattin.dev.ecom.service.infra.CookieService;
import selahattin.dev.ecom.service.infra.TokenService;

@Service
@RequiredArgsConstructor
public class UserService {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CookieService cookieService;
    private final HttpServletRequest request;
    // --- AUTHENTICATION FLOW METHODS ---

    @Transactional
    public UserEntity signup(SignupRequest signupRequest) {
        // Sadece AKTİF kullanıcı var mı diye bakıyoruz.
        // Silinmiş kullanıcı varsa, yeni kayıt oluşturulmasına izin veriyoruz.
        if (userRepository.existsByEmailAndDeletedAtIsNull(signupRequest.getEmail())) {
            throw new UserAlreadyExistsException(signupRequest.getEmail());
        }

        RoleEntity customerRole = roleRepository.findByName("customer")
                .orElseThrow(() -> new ResourceNotFoundException("Sistem rolü bulunamadı: customer"));

        UserEntity user = UserEntity.builder()
                .email(signupRequest.getEmail())
                .firstName(signupRequest.getFirstName())
                .lastName(signupRequest.getLastName())
                .phoneNumber("") // TODO: NULL OLACAK
                .roles(Set.of(customerRole))
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public void activateUser(String email) {
        UserEntity user = findByEmail(email);
        user.setEmailVerifiedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    public UserEntity findByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    // --- USER PROFILE METHODS ---

    public CurrentUserResponse getCurrentUserInfo() {
        UserEntity user = getCurrentUser();
        return mapToResponse(user);
    }

    @Transactional
    public CurrentUserResponse updateProfile(UpdateProfileRequest request) {
        UserEntity currentUser = getCurrentUser();

        if (StringUtils.hasText(request.getFirstName())) {
            currentUser.setFirstName(request.getFirstName());
        }

        if (StringUtils.hasText(request.getLastName())) {
            currentUser.setLastName(request.getLastName());
        }

        if (StringUtils.hasText(request.getPhoneNumber())) {
            String newPhone = request.getPhoneNumber();
            String oldPhone = currentUser.getPhoneNumber();

            // Başka bir aktif kullanıcı bu numarayı kullanıyor mu?
            if (!newPhone.equals(oldPhone) && userRepository.existsByPhoneNumberAndDeletedAtIsNull(newPhone)) {
                throw new UserAlreadyExistsException("Bu telefon numarası zaten kullanımda.");
            }
            currentUser.setPhoneNumber(newPhone);
        }

        UserEntity savedUser = userRepository.save(currentUser);
        return mapToResponse(savedUser);
    }

    // --- SESSION METHODS ---

    public List<SessionResponse> getCurrentUserSessions() {
        UserEntity currentUser = getCurrentUser();
        String currentDeviceId = cookieService.extractDeviceId(request);

        return tokenService.getUserSessions(currentUser.getEmail(), currentDeviceId);
    }

    public void deleteCurrentUserSession(String deviceId) {
        UserEntity currentUser = getCurrentUser();
        tokenService.deleteToken(currentUser.getEmail(), deviceId);
    }

    public void deleteAllCurrentUserSessions() {
        UserEntity currentUser = getCurrentUser();
        tokenService.deleteAllTokens(currentUser.getEmail());
    }

    // --- HELPER METHODS ---

    private CurrentUserResponse mapToResponse(UserEntity user) {
        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .toList();

        return CurrentUserResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roles)
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationContextException("Oturum bulunamadı, lütfen giriş yapın.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            // Context'teki user ID'yi alıp DB'den en güncel halini (ve ilişkilerini)
            // çekiyoruz.
            // Lazy loading hatası almamak ve transaction bütünlüğü için bu daha güvenli.
            UUID userId = userDetails.getUser().getId();
            return userRepository.findById(userId)
                    .orElseThrow(() -> new AuthenticationContextException("Kullanıcı bulunamadı."));
        }

        throw new AuthenticationContextException("Kullanıcı kimliği doğrulanamadı.");
    }

}

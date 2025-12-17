package selahattin.dev.ecom.service.domain;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Spring'in yardımcı sınıfı

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.SignupRequest;
import selahattin.dev.ecom.dto.request.UpdateProfileRequest;
import selahattin.dev.ecom.dto.response.CurrentUserResponse;
import selahattin.dev.ecom.entity.UserEntity;
import selahattin.dev.ecom.exception.auth.AuthenticationContextException;
import selahattin.dev.ecom.exception.user.ResourceNotFoundException;
import selahattin.dev.ecom.exception.user.UserAlreadyExistsException;
import selahattin.dev.ecom.repository.UserRepository;
import selahattin.dev.ecom.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // --- AUTHENTICATION FLOW METHODS ---

    @Transactional
    public UserEntity signup(SignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new UserAlreadyExistsException(signupRequest.getEmail());
        }

        UserEntity user = UserEntity.builder()
                .email(signupRequest.getEmail())
                .firstName(signupRequest.getFirstName())
                .lastName(signupRequest.getLastName())
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public void activateUser(String email) {
        UserEntity user = findByEmail(email);
        user.setActivated(true);
        userRepository.save(user);
    }

    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    // --- USER PROFILE METHODS ---

    public CurrentUserResponse getCurrentUserInfo() {
        UserEntity user = getCurrentUser();
        return mapToResponse(user);
    }

    @Transactional
    public CurrentUserResponse updateProfile(UpdateProfileRequest request) {
        UserEntity currentUser = userRepository.findById(getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı."));

        if (StringUtils.hasText(request.getFirstName())) {
            currentUser.setFirstName(request.getFirstName());
        }

        if (StringUtils.hasText(request.getLastName())) {
            currentUser.setLastName(request.getLastName());
        }

        if (StringUtils.hasText(request.getPhoneNumber())) {
            String newPhone = request.getPhoneNumber();
            String oldPhone = currentUser.getPhoneNumber();

            if (!newPhone.equals(oldPhone) && userRepository.existsByPhoneNumber(newPhone)) {
                throw new UserAlreadyExistsException("Bu telefon numarası zaten kullanımda.");
            }
            currentUser.setPhoneNumber(newPhone);
        }

        UserEntity savedUser = userRepository.save(currentUser);
        return mapToResponse(savedUser);
    }

    // --- HELPER METHODS ---

    private CurrentUserResponse mapToResponse(UserEntity user) {
        return CurrentUserResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
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
            return userDetails.getUser();
        }

        throw new AuthenticationContextException("Kullanıcı kimliği doğrulanamadı.");
    }
}
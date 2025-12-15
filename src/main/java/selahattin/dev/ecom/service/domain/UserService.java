package selahattin.dev.ecom.service.domain;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.SignupRequest;
import selahattin.dev.ecom.dto.response.CurrentUserResponse;
import selahattin.dev.ecom.entity.UserEntity;
import selahattin.dev.ecom.exception.AuthenticationContextException;
import selahattin.dev.ecom.exception.ResourceNotFoundException;
import selahattin.dev.ecom.exception.UserAlreadyExistsException;
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

        return CurrentUserResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationContextException("Oturum bulunamadı, lütfen giriş yapın.");
        }

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        if (principal == null || principal.getUser() == null) {
            throw new AuthenticationContextException("Oturum bulunamadı, lütfen giriş yapın.");
        }
        return principal.getUser();
    }
}
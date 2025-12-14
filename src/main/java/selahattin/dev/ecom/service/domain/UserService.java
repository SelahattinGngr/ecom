package selahattin.dev.ecom.service.domain;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import selahattin.dev.ecom.dto.request.SignupRequest;
import selahattin.dev.ecom.entity.UserEntity;
import selahattin.dev.ecom.exception.ResourceNotFoundException;
import selahattin.dev.ecom.exception.UserAlreadyExistsException;
import selahattin.dev.ecom.repository.UserRepository;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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

    public void activateUser(String email) {
        UserEntity user = findByEmail(email);
        user.setActivated(true);
        userRepository.save(user);
    }

    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + email));
    }
}

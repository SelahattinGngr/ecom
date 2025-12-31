package selahattin.dev.ecom.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.repository.auth.UserRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                UserEntity user = userRepository.findByEmailAndDeletedAtIsNull(email)
                                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));

                return new CustomUserDetails(user);
        }
}
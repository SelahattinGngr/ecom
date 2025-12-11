package selahattin.dev.ecom.service.domain;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.infra.CookieDto;
import selahattin.dev.ecom.dto.infra.EmailMessageDto;
import selahattin.dev.ecom.dto.request.SigninRequest;
import selahattin.dev.ecom.dto.request.SigninWithPassword;
import selahattin.dev.ecom.dto.request.SignupRequest;
import selahattin.dev.ecom.dto.response.SigninResponse;
import selahattin.dev.ecom.entity.UserEntity;
import selahattin.dev.ecom.security.CustomUserDetails;
import selahattin.dev.ecom.service.infra.CookieService;
import selahattin.dev.ecom.service.infra.RedisQueueService;
import selahattin.dev.ecom.service.infra.TokenService;
import selahattin.dev.ecom.utils.cookie.CookieFactory;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final RedisQueueService redisQueueService;
        private final CookieFactory cookieFactory;
        private final UserService userService;
        private final RedisTemplate<String, Object> redisTemplate;
        private final TokenService tokenService;
        private final CookieService cookieService;

        public void sendLoginOtp(SigninRequest signinRequest) {
                UserEntity user = userService.findByEmail(signinRequest.getEmail());

                String otp = generateOnePercentPassword();

                redisTemplate.opsForValue().set(
                                "login_otp:" + user.getEmail(),
                                otp,
                                5,
                                TimeUnit.MINUTES);

                EmailMessageDto emailMessage = EmailMessageDto.builder()
                                .to(user.getEmail())
                                .subject("Giriş Kodunuz")
                                .content("Sayın " + user.getFirstName() + ",\n\nGiriş kodunuz: " + otp)
                                .build();

                redisQueueService.enqueueEmail(emailMessage);
        }

        public SigninResponse verifyLoginOtp(SigninWithPassword request, HttpServletResponse response) {
                String email = request.getEmail();
                String inputOtp = request.getPassword();

                Object redisOtp = redisTemplate.opsForValue().get("login_otp:" + email);

                if (redisOtp == null || !redisOtp.toString().equals(inputOtp)) {
                        throw new RuntimeException("Geçersiz veya süresi dolmuş kod!");
                }

                UserEntity user = userService.findByEmail(email);

                redisTemplate.delete("login_otp:" + email);

                return generateTokensAndReturnResponse(user, response);
        }

        public void signup(SignupRequest signupRequest) {
                userService.signup(signupRequest);
        }

        public String signOut() {
                return "signed out";
        }

        private String generateOnePercentPassword() {
                return String.format("%06d", new Random().nextInt(1000000));
        }

        private SigninResponse generateTokensAndReturnResponse(UserEntity user, HttpServletResponse response) {
                CustomUserDetails userDetails = new CustomUserDetails(user);

                CookieDto cookieDto = cookieFactory.create(userDetails);
                tokenService.storeToken(user.getEmail(), cookieDto);
                cookieService.createCookies(cookieDto, response);

                return SigninResponse.builder()
                                .email(user.getEmail())
                                .role(user.getRole().name())
                                .build();
        }
}
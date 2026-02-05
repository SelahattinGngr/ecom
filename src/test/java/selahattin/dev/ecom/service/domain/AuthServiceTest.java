package selahattin.dev.ecom.service.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import selahattin.dev.ecom.config.properties.ClientProperties;
import selahattin.dev.ecom.dto.infra.CookieDto;
import selahattin.dev.ecom.dto.infra.EmailMessageDto;
import selahattin.dev.ecom.dto.request.SigninRequest;
import selahattin.dev.ecom.dto.request.SignupRequest;
import selahattin.dev.ecom.dto.request.VerifyEmailRequest;
import selahattin.dev.ecom.dto.request.VerifyOtpRequest;
import selahattin.dev.ecom.dto.response.SigninResponse;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.security.CustomUserDetails;
import selahattin.dev.ecom.security.jwt.JwtTokenProvider;
import selahattin.dev.ecom.service.infra.CookieService;
import selahattin.dev.ecom.service.infra.RedisQueueService;
import selahattin.dev.ecom.service.infra.TokenService;
import selahattin.dev.ecom.utils.constant.AuthConstant;
import selahattin.dev.ecom.utils.cookie.CookieFactory;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RedisQueueService redisQueueService;
    @Mock
    private CookieFactory cookieFactory;
    @Mock
    private UserService userService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private TokenService tokenService;
    @Mock
    private CookieService cookieService;
    @Mock
    private ClientProperties clientProperties;

    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private HttpServletResponse httpResponse;

    @InjectMocks
    private AuthService authService;

    @Test
    void sendLoginOtp_ShouldEnqueueEmail_WhenUserExists() {
        // Arrange
        String email = "test@example.com";
        SigninRequest request = new SigninRequest();
        request.setEmail(email);

        UserEntity user = new UserEntity();
        user.setEmail(email);

        when(userService.findByEmail(email)).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        authService.sendLoginOtp(request);

        // Assert
        verify(userService).findByEmail(email);
        verify(valueOperations).set(
                eq(AuthConstant.OTP_KEY_TEMPLATE + email),
                anyString(),
                eq((long) AuthConstant.OTP_DURATION_MINUTES),
                eq(TimeUnit.MINUTES));
        verify(redisQueueService).enqueueEmail(any(EmailMessageDto.class));
    }

    @Test
    void verifyLoginOtp_ShouldReturnTokens_WhenOtpIsValid() {
        // Arrange
        String email = "test@example.com";
        String otp = "123456";
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail(email);
        request.setCode(otp);

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setRoles(new HashSet<>(List.of(new RoleEntity())));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AuthConstant.OTP_KEY_TEMPLATE + email)).thenReturn(otp);
        when(userService.findByEmail(email)).thenReturn(user);
        when(cookieFactory.create(any(CustomUserDetails.class), anyString()))
                .thenReturn(new CookieDto().builder().build());
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        SigninResponse response = authService.verifyLoginOtp(request, httpResponse, httpRequest);

        // Assert
        assertThat(response.getEmail()).isEqualTo(email);
        verify(tokenService).storeSession(eq(user), any(), anyString(), anyString());
        verify(redisTemplate).delete(AuthConstant.OTP_KEY_TEMPLATE + email);
    }

    @Test
    void verifyLoginOtp_ShouldThrowException_WhenOtpIsInvalid() {
        // Arrange
        String email = "test@example.com";
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail(email);
        request.setCode("wrong-otp");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AuthConstant.OTP_KEY_TEMPLATE + email)).thenReturn("123456");

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyLoginOtp(request, httpResponse, httpRequest))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void signup_ShouldEnqueueVerificationEmail_WhenRequestIsValid() {
        // Arrange
        SignupRequest request = new SignupRequest();
        request.setEmail("new@example.com");

        UserEntity user = new UserEntity();
        user.setEmail("new@example.com");

        when(userService.signup(request)).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(clientProperties.getFrontendUrl()).thenReturn("http://localhost:3000");
        when(clientProperties.getEmailVerificationPath()).thenReturn("/verify/");

        // Act
        authService.signup(request);

        // Assert
        verify(userService).signup(request);
        verify(valueOperations).set(
                anyString(), // Key contains UUID
                eq("new@example.com"),
                eq((long) AuthConstant.SIGNUP_TOKEN_DURATION_HOURS),
                eq(TimeUnit.HOURS));
        verify(redisQueueService).enqueueEmail(any(EmailMessageDto.class));
    }

    @Test
    void verifySignup_ShouldActivateUser_WhenTokenIsValid() {
        // Arrange
        String token = UUID.randomUUID().toString();
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(token);
        String email = "pending@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AuthConstant.SIGNUP_KEY_TEMPLATE + token)).thenReturn(email);

        // Act
        authService.verifySignup(request);

        // Assert
        verify(userService).activateUser(email);
        verify(redisTemplate).delete(AuthConstant.SIGNUP_KEY_TEMPLATE + token);
    }

    @Test
    void verifySignup_ShouldThrowException_WhenTokenIsInvalid() {
        // Arrange
        String token = "invalid-token";
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(token);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AuthConstant.SIGNUP_KEY_TEMPLATE + token)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> authService.verifySignup(request))
                .isInstanceOf(BusinessException.class);
    }
}

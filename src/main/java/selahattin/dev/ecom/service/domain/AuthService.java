package selahattin.dev.ecom.service.domain;

import static selahattin.dev.ecom.utils.constant.AuthConstant.OTP_DURATION_MINUTES;
import static selahattin.dev.ecom.utils.constant.AuthConstant.OTP_KEY_TEMPLATE;
import static selahattin.dev.ecom.utils.constant.AuthConstant.SIGNUP_KEY_TEMPLATE;
import static selahattin.dev.ecom.utils.constant.AuthConstant.SIGNUP_TOKEN_DURATION_HOURS;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.config.properties.ClientProperties;
import selahattin.dev.ecom.dto.infra.CookieDto;
import selahattin.dev.ecom.dto.infra.EmailMessageDto;
import selahattin.dev.ecom.dto.request.SigninRequest;
import selahattin.dev.ecom.dto.request.SigninWithPassword;
import selahattin.dev.ecom.dto.request.SignupRequest;
import selahattin.dev.ecom.dto.request.VerifyEmailRequest;
import selahattin.dev.ecom.dto.response.SigninResponse;
import selahattin.dev.ecom.entity.UserEntity;
import selahattin.dev.ecom.exception.InvalidOtpException;
import selahattin.dev.ecom.exception.InvalidRefreshTokenException;
import selahattin.dev.ecom.exception.InvalidSignupVerificationTokenException;
import selahattin.dev.ecom.exception.NotfoundDeviceException;
import selahattin.dev.ecom.exception.SessionExpiredException;
import selahattin.dev.ecom.security.CustomUserDetails;
import selahattin.dev.ecom.security.jwt.JwtTokenProvider;
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
	private final JwtTokenProvider jwtTokenProvider;
	private final TokenService tokenService;
	private final CookieService cookieService;
	private final ClientProperties clientProperties;

	/* --- Signin --- */
	public void sendLoginOtp(SigninRequest signinRequest) {
		String otp = generateOtp();
		UserEntity user = userService.findByEmail(signinRequest.getEmail());

		saveToRedis(OTP_KEY_TEMPLATE + user.getEmail(), otp, OTP_DURATION_MINUTES, TimeUnit.MINUTES);

		EmailMessageDto emailMessage = createEmailMessage(
				user.getEmail(),
				"Giriş Doğrulama Kodu",
				"Giriş yapmak için doğrulama kodunuz: " + otp);

		redisQueueService.enqueueEmail(emailMessage);
	}

	public SigninResponse verifyLoginOtp(SigninWithPassword request, HttpServletResponse response) {
		String email = request.getEmail();
		String inputOtp = request.getPassword();

		validateRedisValue(OTP_KEY_TEMPLATE + email, inputOtp);

		UserEntity user = userService.findByEmail(email);
		deleteFromRedis(OTP_KEY_TEMPLATE + email);

		return generateTokensAndReturnResponse(user, response);
	}
	/* --- --- */

	/* --- Signup --- */
	public void signup(SignupRequest signupRequest) {
		UserEntity user = userService.signup(signupRequest);
		String token = UUID.randomUUID().toString();

		saveToRedis(SIGNUP_KEY_TEMPLATE + token, user.getEmail(), SIGNUP_TOKEN_DURATION_HOURS, TimeUnit.HOURS);

		String verificationLink = clientProperties.getFrontendUrl()
				+ clientProperties.getEmailVerificationPath() + token;

		EmailMessageDto emailMessage = createEmailMessage(
				user.getEmail(),
				"E-posta Doğrulama",
				"Kayıt işleminizi tamamlamak için lütfen aşağıdaki linke tıklayın: \n" + verificationLink);

		redisQueueService.enqueueEmail(emailMessage);
	}

	public void resendVerificationEmail(SigninRequest signinRequest) {
		UserEntity user = userService.findByEmail(signinRequest.getEmail());

		if (user.isActivated()) {
			throw new InvalidSignupVerificationTokenException("Kullanıcı zaten aktif.");
		}

		String token = UUID.randomUUID().toString();

		saveToRedis(SIGNUP_KEY_TEMPLATE + token, user.getEmail(), SIGNUP_TOKEN_DURATION_HOURS, TimeUnit.HOURS);

		String verificationLink = clientProperties.getFrontendUrl()
				+ clientProperties.getEmailVerificationPath() + token;

		EmailMessageDto emailMessage = createEmailMessage(
				user.getEmail(),
				"E-posta Doğrulama - Tekrar Gönderildi",
				"Kayıt işleminizi tamamlamak için lütfen aşağıdaki linke tıklayın: \n" + verificationLink);

		redisQueueService.enqueueEmail(emailMessage);
	}

	public void verifySignup(VerifyEmailRequest request) {
		String token = request.getToken();
		String redisKey = SIGNUP_KEY_TEMPLATE + token;

		String email = (String) getFromRedis(redisKey);

		if (email == null) {
			throw new InvalidSignupVerificationTokenException("Geçersiz veya süresi dolmuş doğrulama linki!");
		}

		userService.activateUser(email);
		deleteFromRedis(redisKey);
	}
	/* --- --- */

	/* --- Signout --- */
	public void signOut(String deviceId, String refreshToken, HttpServletResponse response) {
		try {
			String email = jwtTokenProvider.extractUsername(refreshToken, false);
			tokenService.deleteToken(email, deviceId);
		} catch (Exception e) {
			// Logla ama kullanıcıya çaktırma, cookie silinsin yeter
		}
		cookieService.clearCookies(response);
	}
	/* --- --- */

	/* --- Token Operations --- */
	public void refreshToken(String refreshToken, HttpServletResponse response) {
		if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
			throw new InvalidRefreshTokenException("Geçersiz Refresh Token.");
		}

		String email = jwtTokenProvider.extractUsername(refreshToken, false);
		String deviceId = jwtTokenProvider.extractDeviceId(refreshToken);

		if (deviceId == null) {
			throw new NotfoundDeviceException("Token içinde Cihaz bilgisi bulunamadı.");
		}

		if (!tokenService.validateToken(email, deviceId, refreshToken)) {
			throw new SessionExpiredException("Oturum sonlandırılmış.");
		}

		UserEntity user = userService.findByEmail(email);

		generateTokensAndReturnResponse(user, deviceId, response);
	}
	/* --- --- */

	/**
	 * Helper Methods
	 */
	private String generateOtp() {
		return String.format("%06d", new SecureRandom().nextInt(1000000));
	}

	private SigninResponse generateTokensAndReturnResponse(UserEntity user, HttpServletResponse response) {
		String deviceId = UUID.randomUUID().toString();
		return generateTokensAndReturnResponse(user, deviceId, response);
	}

	private SigninResponse generateTokensAndReturnResponse(UserEntity user, String deviceId,
			HttpServletResponse response) {
		CustomUserDetails userDetails = new CustomUserDetails(user);

		CookieDto cookieDto = cookieFactory.create(userDetails, deviceId);

		tokenService.storeToken(user.getEmail(), cookieDto);
		cookieService.createCookies(cookieDto, response);

		return SigninResponse.builder()
				.email(user.getEmail())
				.role(user.getRole().name())
				.build();
	}

	/**
	 * Redis Operations
	 */
	private void saveToRedis(String key, Object value, long duration, TimeUnit unit) {
		redisTemplate.opsForValue().set(key, value, duration, unit);
	}

	private Object getFromRedis(String key) {
		return redisTemplate.opsForValue().get(key);
	}

	private void deleteFromRedis(String key) {
		redisTemplate.delete(key);
	}

	private void validateRedisValue(String key, String expectedValue) {
		Object actualValue = redisTemplate.opsForValue().get(key);
		if (actualValue == null || !actualValue.toString().equals(expectedValue)) {
			throw new InvalidOtpException("Geçersiz veya süresi dolmuş kod!");
		}
	}

	private EmailMessageDto createEmailMessage(String to, String subject, String content) {
		return EmailMessageDto.builder()
				.to(to)
				.subject(subject)
				.content(content)
				.build();
	}
}
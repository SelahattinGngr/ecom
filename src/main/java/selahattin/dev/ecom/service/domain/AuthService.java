package selahattin.dev.ecom.service.domain;

import static selahattin.dev.ecom.utils.constant.AuthConstant.OTP_DURATION_MINUTES;
import static selahattin.dev.ecom.utils.constant.AuthConstant.OTP_KEY_TEMPLATE;
import static selahattin.dev.ecom.utils.constant.AuthConstant.SIGNUP_KEY_TEMPLATE;
import static selahattin.dev.ecom.utils.constant.AuthConstant.SIGNUP_TOKEN_DURATION_HOURS;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
import selahattin.dev.ecom.exception.auth.InvalidOtpException;
import selahattin.dev.ecom.exception.auth.InvalidRefreshTokenException;
import selahattin.dev.ecom.exception.auth.InvalidSignupVerificationTokenException;
import selahattin.dev.ecom.exception.auth.NotfoundDeviceException;
import selahattin.dev.ecom.exception.auth.SessionExpiredException;
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

	/* --- Signin (OTP Flow) --- */
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

	public SigninResponse verifyLoginOtp(VerifyOtpRequest request, HttpServletResponse response,
			HttpServletRequest httpRequest) {
		String email = request.getEmail();
		String inputOtp = request.getCode();

		validateRedisValue(OTP_KEY_TEMPLATE + email, inputOtp);

		UserEntity user = userService.findByEmail(email);
		deleteFromRedis(OTP_KEY_TEMPLATE + email);

		// IP ve UA çekiyoruz
		String ip = getClientIp(httpRequest);
		String ua = getUserAgent(httpRequest);

		return generateTokensAndReturnResponse(user, response, ip, ua);
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

	public void resendVerificationEmail(SigninRequest signinRequest) {
		UserEntity user = userService.findByEmail(signinRequest.getEmail());

		if (user.getEmailVerifiedAt() != null) {
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
	/* --- --- */

	/* --- Signout --- */
	public void signOut(String deviceId, String refreshToken, HttpServletResponse response) {
		try {
			String email = jwtTokenProvider.extractUsername(refreshToken, false);
			tokenService.deleteToken(email, deviceId);
		} catch (Exception e) {
			// Token bozuksa bile cookie temizle
		}
		cookieService.clearCookies(response);
	}
	/* --- --- */

	/* --- Token Operations --- */
	public void refreshToken(String refreshToken, HttpServletResponse response, HttpServletRequest httpRequest) {
		if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
			throw new InvalidRefreshTokenException("Geçersiz Refresh Token.");
		}

		String email = jwtTokenProvider.extractUsername(refreshToken, false);
		String deviceId = jwtTokenProvider.extractDeviceId(refreshToken, false);

		if (deviceId == null) {
			throw new NotfoundDeviceException("Token içinde Cihaz bilgisi bulunamadı.");
		}

		if (!tokenService.validateToken(email, deviceId, refreshToken)) {
			throw new SessionExpiredException("Oturum sonlandırılmış.");
		}

		UserEntity user = userService.findByEmail(email);

		String ip = getClientIp(httpRequest);
		String ua = getUserAgent(httpRequest);

		generateTokensAndReturnResponse(user, deviceId, response, ip, ua);
	}
	/* --- --- */

	/**
	 * Helper Methods
	 */
	private String generateOtp() {
		return String.format("%06d", new SecureRandom().nextInt(1000000));
	}

	private SigninResponse generateTokensAndReturnResponse(UserEntity user, HttpServletResponse response, String ip,
			String ua) {
		String deviceId = UUID.randomUUID().toString();
		return generateTokensAndReturnResponse(user, deviceId, response, ip, ua);
	}

	private SigninResponse generateTokensAndReturnResponse(UserEntity user, String deviceId,
			HttpServletResponse response, String ipAddress, String userAgent) {

		List<String> roles = user.getRoles().stream().map(RoleEntity::getName).toList();
		CustomUserDetails userDetails = new CustomUserDetails(user);
		CookieDto cookieDto = cookieFactory.create(userDetails, deviceId);

		tokenService.storeSession(user, cookieDto, ipAddress, userAgent);

		cookieService.createCookies(cookieDto, response);

		return SigninResponse.builder()
				.email(user.getEmail())
				.roles(roles)
				.build();
	}

	private String getClientIp(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader == null) {
			return request.getRemoteAddr();
		}
		return xfHeader.split(",")[0];
	}

	private String getUserAgent(HttpServletRequest request) {
		String ua = request.getHeader("User-Agent");
		return ua != null ? ua : "Unknown";
	}
	// ------------------------------

	/* Redis Helpers */
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
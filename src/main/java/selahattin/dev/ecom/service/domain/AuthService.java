package selahattin.dev.ecom.service.domain;

import static selahattin.dev.ecom.utils.constant.AuthConstant.MAX_OTP_ATTEMPTS;
import static selahattin.dev.ecom.utils.constant.AuthConstant.MAX_OTP_RATE_PER_MINUTE;
import static selahattin.dev.ecom.utils.constant.AuthConstant.MAX_SIGNUP_RATE_PER_HOUR;
import static selahattin.dev.ecom.utils.constant.AuthConstant.OTP_ATTEMPTS_KEY_TEMPLATE;
import static selahattin.dev.ecom.utils.constant.AuthConstant.OTP_DURATION_MINUTES;
import static selahattin.dev.ecom.utils.constant.AuthConstant.OTP_KEY_TEMPLATE;
import static selahattin.dev.ecom.utils.constant.AuthConstant.RATE_LIMIT_KEY_TEMPLATE;
import static selahattin.dev.ecom.utils.constant.AuthConstant.SIGNUP_KEY_TEMPLATE;
import static selahattin.dev.ecom.utils.constant.AuthConstant.SIGNUP_RATE_KEY_TEMPLATE;
import static selahattin.dev.ecom.utils.constant.AuthConstant.SIGNUP_TOKEN_DURATION_HOURS;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.security.CustomUserDetails;
import selahattin.dev.ecom.security.jwt.JwtTokenProvider;
import selahattin.dev.ecom.service.infra.CookieService;
import selahattin.dev.ecom.service.infra.RedisQueueService;
import selahattin.dev.ecom.service.infra.TokenService;
import selahattin.dev.ecom.utils.cookie.CookieFactory;
import selahattin.dev.ecom.utils.enums.SecurityEventType;

@Slf4j
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
	private final SecurityEventService securityEventService;

	/* --- Signin (OTP Flow) --- */
	public void sendLoginOtp(SigninRequest signinRequest) {
		checkOtpRateLimit(signinRequest.getEmail());
		String otp = generateOtp();
		UserEntity user = userService.findByEmail(signinRequest.getEmail());

		if (user.getEmailVerifiedAt() == null) {
			throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED,
					"E-posta adresiniz doğrulanmamış. Lütfen önce e-postanızı doğrulayın.");
		}

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
		String ip = getClientIp(httpRequest);
		String ua = getUserAgent(httpRequest);
		String attemptsKey = OTP_ATTEMPTS_KEY_TEMPLATE + email;

		Object attemptsObj = redisTemplate.opsForValue().get(attemptsKey);
		if (attemptsObj != null && Long.parseLong(attemptsObj.toString()) >= MAX_OTP_ATTEMPTS) {
			deleteFromRedis(OTP_KEY_TEMPLATE + email);
			throw new BusinessException(ErrorCode.INVALID_OTP);
		}

		try {
			validateRedisValue(OTP_KEY_TEMPLATE + email, inputOtp);
		} catch (BusinessException e) {
			Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
			if (attempts == 1) {
				redisTemplate.expire(attemptsKey, OTP_DURATION_MINUTES, TimeUnit.MINUTES);
			}
			if (attempts >= MAX_OTP_ATTEMPTS) {
				deleteFromRedis(OTP_KEY_TEMPLATE + email);
			}
			try {
				securityEventService.log(SecurityEventType.LOGIN_FAILED, null, email, ip, ua,
						Map.of("reason", "INVALID_OTP"));
			} catch (Exception logEx) {
				log.warn("[AUTH] LOGIN_FAILED event yazılamadı — email: {}", email, logEx);
			}
			throw e;
		}

		deleteFromRedis(attemptsKey);
		UserEntity user = userService.findByEmail(email);
		deleteFromRedis(OTP_KEY_TEMPLATE + email);

		SigninResponse result = generateTokensAndReturnResponse(user, response, ip, ua);

		try {
			securityEventService.log(SecurityEventType.LOGIN_SUCCESS, user.getId(), user.getEmail(), ip, ua,
					Map.of());
		} catch (Exception logEx) {
			log.warn("[AUTH] LOGIN_SUCCESS event yazılamadı — userId: {}", user.getId(), logEx);
		}

		log.info("[AUTH] Kullanıcı giriş yaptı — userId: {}, IP: {}", user.getId(), ip);
		return result;
	}
	/* --- --- */

	/* --- Signup --- */
	public void signup(SignupRequest signupRequest) {
		checkSignupRateLimit(signupRequest.getEmail());
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
			throw new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN);
		}

		userService.activateUser(email);
		deleteFromRedis(redisKey);

		try {
			UserEntity user = userService.findByEmail(email);
			securityEventService.log(SecurityEventType.SIGNUP_COMPLETED, user.getId(), email, null, null,
					Map.of());
		} catch (Exception logEx) {
			log.warn("[AUTH] SIGNUP_COMPLETED event yazılamadı — email: {}", email, logEx);
		}

		log.info("[AUTH] Email doğrulandı — email: {}", email);
	}

	public void resendVerificationEmail(SigninRequest signinRequest) {
		checkOtpRateLimit(signinRequest.getEmail());
		UserEntity user = userService.findByEmail(signinRequest.getEmail());

		if (user.getEmailVerifiedAt() != null) {
			throw new BusinessException(ErrorCode.USER_ALREADY_VERIFIED);
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

			try {
				UserEntity user = userService.findByEmail(email);
				securityEventService.log(SecurityEventType.LOGOUT, user.getId(), email, null, null,
						Map.of("deviceId", deviceId != null ? deviceId : ""));
			} catch (Exception logEx) {
				log.warn("[AUTH] LOGOUT event yazılamadı — email: {}", email, logEx);
			}

			log.info("[AUTH] Kullanıcı çıkış yaptı — email: {}, deviceId: {}", email, deviceId);
		} catch (Exception e) {
			// Token bozuksa bile cookie temizle
		}
		cookieService.clearCookies(response);
	}
	/* --- --- */

	/* --- Token Operations --- */
	public void refreshToken(String refreshToken, HttpServletResponse response, HttpServletRequest httpRequest) {
		String ip = getClientIp(httpRequest);
		String ua = getUserAgent(httpRequest);

		if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
			try {
				securityEventService.log(SecurityEventType.TOKEN_REFRESH_FAILED, null, null, ip, ua,
						Map.of("reason", "INVALID_REFRESH_TOKEN"));
			} catch (Exception logEx) {
				log.warn("[AUTH] TOKEN_REFRESH_FAILED event yazılamadı — IP: {}", ip, logEx);
			}
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		String email = jwtTokenProvider.extractUsername(refreshToken, false);
		String deviceId = jwtTokenProvider.extractDeviceId(refreshToken, false);

		if (deviceId == null) {
			throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND, "Token içinde Cihaz bilgisi bulunamadı.");
		}

		if (!tokenService.validateToken(email, deviceId, refreshToken)) {
			throw new BusinessException(ErrorCode.SESSION_EXPIRED);
		}

		UserEntity user = userService.findByEmail(email);

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
		return request.getRemoteAddr();
	}

	private String getUserAgent(HttpServletRequest request) {
		String ua = request.getHeader("User-Agent");
		return ua != null ? ua : "Unknown";
	}
	// ------------------------------

	private void checkSignupRateLimit(String email) {
		String key = SIGNUP_RATE_KEY_TEMPLATE + email;
		Long count = redisTemplate.opsForValue().increment(key);
		if (count == 1) {
			redisTemplate.expire(key, 1, TimeUnit.HOURS);
		}
		if (count > MAX_SIGNUP_RATE_PER_HOUR) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "Bu e-posta ile çok fazla kayıt denemesi yapıldı. Lütfen 1 saat bekleyin.");
		}
	}

	private void checkOtpRateLimit(String email) {
		String key = RATE_LIMIT_KEY_TEMPLATE + email;
		Long count = redisTemplate.opsForValue().increment(key);
		if (count == 1) {
			redisTemplate.expire(key, 1, TimeUnit.MINUTES);
		}
		if (count > MAX_OTP_RATE_PER_MINUTE) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "Çok fazla istek gönderildi. Lütfen 1 dakika bekleyin.");
		}
	}

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
			throw new BusinessException(ErrorCode.INVALID_OTP);
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

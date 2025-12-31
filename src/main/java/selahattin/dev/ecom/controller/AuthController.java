package selahattin.dev.ecom.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.SigninRequest;
import selahattin.dev.ecom.dto.request.SignupRequest;
import selahattin.dev.ecom.dto.request.VerifyEmailRequest;
import selahattin.dev.ecom.dto.request.VerifyOtpRequest;
import selahattin.dev.ecom.dto.response.SigninResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.AuthService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	// --- AUTHENTICATION (OTP FLOW) ---

	// Adım 1: Kullanıcı mailini girer, OTP ister.
	@PostMapping("/public/signin")
	public ResponseEntity<ApiResponse<String>> requestLoginOtp(@Valid @RequestBody SigninRequest signinRequest) {
		authService.sendLoginOtp(signinRequest);
		return ResponseEntity.ok(ApiResponse.success("Doğrulama kodu gönderildi.", "OTP_SENT"));
	}

	// Adım 1.1: Kod gelmediyse tekrar iste.
	@PostMapping("/public/resend-otp")
	public ResponseEntity<ApiResponse<String>> resendLoginOtp(@Valid @RequestBody SigninRequest signinRequest) {
		authService.sendLoginOtp(signinRequest);
		return ResponseEntity.ok(ApiResponse.success("Doğrulama kodu tekrar gönderildi.", "OTP_RESENT"));
	}

	// Adım 2: Kullanıcı mail + kodu girer, Token alır.
	@PostMapping("/public/signin-verify")
	public ResponseEntity<ApiResponse<SigninResponse>> verifyLoginOtp(HttpServletResponse response,
			HttpServletRequest request,
			@Valid @RequestBody VerifyOtpRequest verifyRequest) {
		return ResponseEntity
				.ok(ApiResponse.success("Giriş Başarılı",
						authService.verifyLoginOtp(verifyRequest, response, request)));
	}

	// --- REGISTRATION ---

	@PostMapping("/public/signup")
	public ResponseEntity<ApiResponse<String>> signUp(@Valid @RequestBody SignupRequest signupRequest) {
		authService.signup(signupRequest);
		return ResponseEntity.ok(ApiResponse.success(
				"Kayıt Başarılı, lütfen e-posta adresinize gelen doğrulama linkine tıklayın.", "SIGNUP_SUCCESS"));
	}

	@PostMapping("/public/signup-verify")
	public ResponseEntity<ApiResponse<String>> verifySignup(@Valid @RequestBody VerifyEmailRequest verifyEmailRequest) {
		authService.verifySignup(verifyEmailRequest);
		return ResponseEntity
				.ok(ApiResponse.success("E-posta doğrulama başarılı, giriş yapabilirsiniz.", "SIGNUP_VERIFIED"));
	}

	@PostMapping("/public/resend-verification-email")
	public ResponseEntity<ApiResponse<String>> resendVerificationEmail(
			@Valid @RequestBody SigninRequest signinRequest) {
		authService.resendVerificationEmail(signinRequest);
		return ResponseEntity
				.ok(ApiResponse.success("Doğrulama e-postası tekrar gönderildi.", "VERIFICATION_EMAIL_RESENT"));
	}

	@PostMapping("/public/refresh-token")
	public ResponseEntity<ApiResponse<String>> refreshToken(
			@CookieValue(required = false) String refreshToken,
			@CookieValue(required = false) String deviceId,
			HttpServletResponse response, HttpServletRequest request) {

		authService.refreshToken(refreshToken, response, request);
		return ResponseEntity.ok(ApiResponse.success("Token yenilendi.", "TOKEN_REFRESHED"));
	}

	@PostMapping("/signout")
	public ResponseEntity<ApiResponse<String>> signOut(
			@CookieValue(required = false) String deviceId,
			@CookieValue(required = false) String refreshToken,
			HttpServletResponse response) {

		authService.signOut(deviceId, refreshToken, response);
		return ResponseEntity.ok(ApiResponse.success("Çıkış Başarılı", "SIGNOUT_SUCCESS"));
	}
}
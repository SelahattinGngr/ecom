package selahattin.dev.ecom.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.SigninRequest;
import selahattin.dev.ecom.dto.request.SigninWithPassword;
import selahattin.dev.ecom.dto.request.SignupRequest;
import selahattin.dev.ecom.dto.request.VerifyEmailRequest;
import selahattin.dev.ecom.dto.response.SigninResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	// --- AUTHENTICATION ---

	@PostMapping("/public/signin")
	public ResponseEntity<ApiResponse<String>> requestLoginOtp(@Valid @RequestBody SigninRequest signinRequest) {
		authService.sendLoginOtp(signinRequest);
		return ResponseEntity.ok(ApiResponse.success("Doğrulama kodu gönderildi.", "OTP_SENT"));
	}

	@PostMapping("/public/resend-otp")
	public ResponseEntity<ApiResponse<String>> resendLoginOtp(@Valid @RequestBody SigninRequest signinRequest) {
		authService.sendLoginOtp(signinRequest);
		return ResponseEntity.ok(ApiResponse.success("Doğrulama kodu tekrar gönderildi.", "OTP_RESENT"));
	}

	@PostMapping("/public/signin-verify")
	public ResponseEntity<ApiResponse<SigninResponse>> verifyLoginOtp(HttpServletResponse response,
			@Valid @RequestBody SigninWithPassword signinRequest) {
		return ResponseEntity
				.ok(ApiResponse.success("Giriş Başarılı", authService.verifyLoginOtp(signinRequest, response)));
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

	@PostMapping("/refresh-token")
	public ResponseEntity<ApiResponse<String>> refreshToken(
			@CookieValue(name = "refreshToken", required = false) String refreshToken,
			@CookieValue(name = "deviceId", required = false) String deviceId,
			HttpServletResponse response) {

		authService.refreshToken(refreshToken, response);
		return ResponseEntity.ok(ApiResponse.success("Token yenilendi.", "TOKEN_REFRESHED"));
	}

	@PostMapping("/signout")
	public ResponseEntity<ApiResponse<String>> signOut(
			@CookieValue(name = "deviceId", required = false) String deviceId,
			@CookieValue(name = "refreshToken", required = false) String refreshToken,
			HttpServletResponse response) {

		authService.signOut(deviceId, refreshToken, response);
		return ResponseEntity.ok(ApiResponse.success("Çıkış Başarılı", "SIGNOUT_SUCCESS"));
	}
}
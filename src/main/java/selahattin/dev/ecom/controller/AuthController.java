package selahattin.dev.ecom.controller;

import org.springframework.http.ResponseEntity;
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
import selahattin.dev.ecom.dto.response.SigninResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/public/signin")
    public ResponseEntity<ApiResponse<String>> requestLoginOtp(@Valid @RequestBody SigninRequest signinRequest) {
        authService.sendLoginOtp(signinRequest);
        return ResponseEntity.ok(ApiResponse.success("Doğrulama kodu mail adresinize gönderildi.", "OTP_SENT"));
    }

    @PostMapping("/public/signin-verify")
    public ResponseEntity<ApiResponse<SigninResponse>> verifyLoginOtp(HttpServletResponse response,
            @Valid @RequestBody SigninWithPassword signinRequest) {
        return ResponseEntity
                .ok(ApiResponse.success("Giriş Başarılı", authService.verifyLoginOtp(signinRequest, response)));
    }

    @PostMapping("/public/signup")
    public ResponseEntity<ApiResponse<String>> signUp(@Valid @RequestBody SignupRequest signupRequest) {
        authService.signup(signupRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Kayıt Başarılı, lütfen e-posta adresinize gelen doğrulama linkine tıklayın.", "SIGNUP_SUCCESS"));
    }

    @PostMapping("/signout")
    public ResponseEntity<ApiResponse<String>> signOut() {
        return ResponseEntity.ok(ApiResponse.success("Çıkış Başarılı", authService.signOut()));
    }
}
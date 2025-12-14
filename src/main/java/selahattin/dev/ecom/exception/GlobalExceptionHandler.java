package selahattin.dev.ecom.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.response.ApiResponse;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
                        MethodArgumentNotValidException ex) {
                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getFieldErrors()
                                .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));

                log.warn("Validation failed: {}", errors);

                return ResponseEntity.badRequest()
                                .body(ApiResponse.error("Validasyon hatası", "VALIDATION_ERROR"));
        }

        @ExceptionHandler(MissingRequestCookieException.class)
        public ResponseEntity<ApiResponse<Object>> handleMissingCookie(MissingRequestCookieException ex) {
                log.warn("Eksik Cookie: {}", ex.getCookieName());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error("Gerekli cookie bulunamadı: " + ex.getCookieName(),
                                                "MISSING_COOKIE"));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error("Kullanıcı adı veya şifre hatalı", "BAD_CREDENTIALS"));
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error(ex.getMessage(), "UNAUTHORIZED"));
        }

        @ExceptionHandler(UserAlreadyExistsException.class)
        public ResponseEntity<ApiResponse<Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error(ex.getMessage(), "USER_ALREADY_EXISTS"));
        }

        @ExceptionHandler(InvalidOtpException.class)
        public ResponseEntity<ApiResponse<Object>> handleInvalidOtp(InvalidOtpException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(ex.getMessage(), "INVALID_OTP"));
        }

        @ExceptionHandler(InvalidSignupVerificationTokenException.class)
        public ResponseEntity<ApiResponse<Object>> handleInvalidSignupVerificationToken(
                        InvalidSignupVerificationTokenException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(ex.getMessage(), "INVALID_SIGNUP_VERIFICATION_TOKEN"));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
                log.error("Beklenmeyen Hata: ", ex);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error("Sunucu tarafında beklenmeyen bir hata oluştu.",
                                                "INTERNAL_ERROR"));
        }
}
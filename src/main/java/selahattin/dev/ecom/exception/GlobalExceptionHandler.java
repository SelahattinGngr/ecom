package selahattin.dev.ecom.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.response.ApiResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// BİZİM FIRLATTIĞIMIZ HATALAR (Business Logic)
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
		ErrorCode errorCode = ex.getErrorCode();
		log.warn("Business Error: Code: {}, Message: {}", errorCode.getCode(), ex.getMessage());

		return ResponseEntity
				.status(errorCode.getHttpStatus())
				.body(ApiResponse.error(ex.getMessage(), errorCode.getCode()));
	}

	// SPRING VALIDASYON HATALARI (@NotNull, @Size vs.)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
			MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
		log.warn("Validation Error: {}", errors);

		return ResponseEntity
				.status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
				.body(ApiResponse.<Map<String, String>>builder()
						.success(false)
						.message("Doğrulama hatası")
						.errorCode(ErrorCode.VALIDATION_ERROR.getCode())
						.data(errors)
						.build());
	}

	// SPRING SECURITY HATALARI (BadCredentials)
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
		return ResponseEntity
				.status(ErrorCode.BAD_CREDENTIALS.getHttpStatus())
				.body(ApiResponse.error("Kullanıcı adı veya şifre hatalı", ErrorCode.BAD_CREDENTIALS.getCode()));
	}

	// EKSİK COOKIE HATASI
	@ExceptionHandler(MissingRequestCookieException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingCookie(MissingRequestCookieException ex) {
		return ResponseEntity
				.status(ErrorCode.MISSING_COOKIE.getHttpStatus())
				.body(ApiResponse.error("Eksik cookie: " + ex.getCookieName(), ErrorCode.MISSING_COOKIE.getCode()));
	}

	// BEKLENMEYEN HATALAR (NullPointer, DB Connection vs.)
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
		log.error("Beklenmeyen Hata: ", ex);
		return ResponseEntity
				.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
				.body(ApiResponse.error("Sunucu tarafında bir hata oluştu: " + ex.getMessage(),
						ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
	}
}
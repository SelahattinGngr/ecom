package selahattin.dev.ecom.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    // YETKİ HATASI (@PreAuthorize)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access Denied: {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.ACCESS_DENIED.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.ACCESS_DENIED.getMessage(), ErrorCode.ACCESS_DENIED.getCode()));
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

    // OLMAYAN ENDPOINT
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity
                .status(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error("Endpoint bulunamadı.", ErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    // BEKLENMEYEN HATALAR (NullPointer, DB Connection vs.)
    // Güvenlik gereği iç hata detayı istemciye açıklanmaz; tam stack trace sadece server log'a yazılır.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Beklenmeyen Hata: ", ex);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(
                        "Beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin.",
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }
}

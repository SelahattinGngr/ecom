package selahattin.dev.ecom.response;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final Instant timestamp;

    private final Integer errorCode;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .errorCode(null) // Başarılıysa hata kodu yok
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .errorCode(null)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, int errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode) // int değeri Integer'a autoboxing olur
                .timestamp(Instant.now())
                .build();
    }
}
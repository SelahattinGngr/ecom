package selahattin.dev.ecom.exception.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthenticationContextException extends RuntimeException {
    public AuthenticationContextException(String message) {
        super(message);
    }
}
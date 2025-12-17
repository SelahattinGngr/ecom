package selahattin.dev.ecom.exception.auth;

public class SessionExpiredException extends RuntimeException {
    public SessionExpiredException(String message) {
        super(message);
    }

}

package selahattin.dev.ecom.exception.auth;

public class InvalidSignupVerificationTokenException extends RuntimeException {
    public InvalidSignupVerificationTokenException(String message) {
        super(message);
    }

}

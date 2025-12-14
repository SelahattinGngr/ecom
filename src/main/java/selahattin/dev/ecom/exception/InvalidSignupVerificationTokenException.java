package selahattin.dev.ecom.exception;

public class InvalidSignupVerificationTokenException extends RuntimeException {
    public InvalidSignupVerificationTokenException(String message) {
        super(message);
    }

}

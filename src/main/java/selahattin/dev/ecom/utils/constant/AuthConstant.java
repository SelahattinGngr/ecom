package selahattin.dev.ecom.utils.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthConstant {

    public static final String REDIS_PREFIX = "auth:";

    // Redis Key Prefixleri
    public static final String SIGNIN_OTP_PREFIX = "signin_otp:";
    public static final String SIGNUP_VERIFY_PREFIX = "signup_verify:";

    // Birleştirilmiş Keyler
    public static final String OTP_KEY_TEMPLATE = REDIS_PREFIX + SIGNIN_OTP_PREFIX;
    public static final String SIGNUP_KEY_TEMPLATE = REDIS_PREFIX + SIGNUP_VERIFY_PREFIX;

    // Süreler
    public static final int OTP_DURATION_MINUTES = 5;
    public static final int SIGNUP_TOKEN_DURATION_HOURS = 1;
}

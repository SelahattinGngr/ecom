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

    // OTP brute-force
    public static final String OTP_ATTEMPTS_PREFIX = "otp_attempts:";
    public static final String OTP_ATTEMPTS_KEY_TEMPLATE = REDIS_PREFIX + OTP_ATTEMPTS_PREFIX;
    public static final int MAX_OTP_ATTEMPTS = 5;

    // OTP rate limit (email başına dakikada maks. istek)
    public static final String RATE_LIMIT_PREFIX = "otprate:";
    public static final String RATE_LIMIT_KEY_TEMPLATE = REDIS_PREFIX + RATE_LIMIT_PREFIX;
    public static final int MAX_OTP_RATE_PER_MINUTE = 3;

    // Signup rate limit (email başına saatte maks. kayıt denemesi)
    public static final String SIGNUP_RATE_PREFIX = "signuprate:";
    public static final String SIGNUP_RATE_KEY_TEMPLATE = REDIS_PREFIX + SIGNUP_RATE_PREFIX;
    public static final int MAX_SIGNUP_RATE_PER_HOUR = 3;
}

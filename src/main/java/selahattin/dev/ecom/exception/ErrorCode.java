package selahattin.dev.ecom.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // --- GENEL & SİSTEM (1000-1999) ---
    INTERNAL_SERVER_ERROR(1000, "Sunucu hatası", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(1001, "Geçersiz istek", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(1002, "Doğrulama hatası", HttpStatus.BAD_REQUEST),
    MISSING_COOKIE(1003, "Gerekli çerez bulunamadı", HttpStatus.BAD_REQUEST),
    USER_ALREADY_VERIFIED(1004, "Kullanıcı zaten doğrulanmış", HttpStatus.BAD_REQUEST),
    COUNTRY_NOT_FOUND(1005, "Ülke bulunamadı", HttpStatus.NOT_FOUND),
    CITY_NOT_FOUND(1006, "Şehir bulunamadı", HttpStatus.NOT_FOUND),
    DISTRICT_NOT_FOUND(1007, "İlçe bulunamadı", HttpStatus.NOT_FOUND),
    DISTRICT_CITY_MISMATCH(1008, "Seçilen ilçe, seçilen şehre ait değil", HttpStatus.BAD_REQUEST),
    JSON_PROCESSING_ERROR(1009, "Veri işleme hatası", HttpStatus.INTERNAL_SERVER_ERROR),
    CRYPTO_ERROR(1010, "Şifreleme algoritması hatası", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- AUTH & SECURITY (2000-2999) ---
    UNAUTHORIZED(2001, "Yetkisiz erişim", HttpStatus.UNAUTHORIZED),
    BAD_CREDENTIALS(2002, "Kullanıcı adı veya şifre hatalı", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED(2003, "Oturum süresi doldu", HttpStatus.UNAUTHORIZED),
    AUTH_CONTEXT_ERROR(2004, "Kimlik doğrulama bağlamı hatası", HttpStatus.UNAUTHORIZED),
    SESSION_NOT_FOUND(2010, "Oturum bulunamadı", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_FOUND(2020, "Adres bulunamadı", HttpStatus.NOT_FOUND),
    ADDRESS_ACCESS_DENIED(2021, "Bu adrese erişim yetkiniz yok", HttpStatus.FORBIDDEN),

    // --- USER & ACCOUNT (3000-3999) ---
    USER_NOT_FOUND(3001, "Kullanıcı bulunamadı", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(3002, "Bu kullanıcı zaten kayıtlı", HttpStatus.CONFLICT),
    USER_PHONE_ALREADY_EXISTS(3010, "Bu telefon numarası zaten kayıtlı", HttpStatus.CONFLICT),
    INVALID_OTP(3003, "Geçersiz veya süresi dolmuş OTP", HttpStatus.BAD_REQUEST),
    INVALID_VERIFICATION_TOKEN(3004, "Geçersiz doğrulama tokeni", HttpStatus.BAD_REQUEST),
    DEVICE_NOT_FOUND(3005, "Cihaz bulunamadı", HttpStatus.NOT_FOUND),
    INVALID_REFRESH_TOKEN(3006, "Geçersiz yenileme tokeni", HttpStatus.BAD_REQUEST),

    // --- CATALOG & PRODUCT (4000-4999) ---
    RESOURCE_NOT_FOUND(4001, "Kaynak bulunamadı", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(4002, "Kategori bulunamadı", HttpStatus.NOT_FOUND),
    DUPLICATE_CATEGORY_NAME(4003, "Kategori ismi zaten kullanımda", HttpStatus.CONFLICT),
    PRODUCT_NOT_FOUND(4004, "Ürün bulunamadı", HttpStatus.NOT_FOUND),
    VARIANT_NOT_FOUND(4005, "Varyant bulunamadı", HttpStatus.NOT_FOUND),
    IMAGE_NOT_FOUND(4006, "Görsel bulunamadı", HttpStatus.NOT_FOUND),
    VARIANT_MISMATCH(4007, "Varyant bu ürüne ait değil", HttpStatus.BAD_REQUEST),

    // --- CART (4500-4599) ---
    CART_ITEM_NOT_FOUND(4501, "Sepetteki ürün bulunamadı", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK(4502, "Yetersiz stok", HttpStatus.BAD_REQUEST),
    MAX_CART_LIMIT_EXCEEDED(4503, "Sepet limiti aşıldı", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_ACTIVE(4505, "Bu ürün şu an satışta değil", HttpStatus.BAD_REQUEST),

    // --- ADMIN & ROLES (5000-5999) ---
    ROLE_ASSIGNED_TO_USER(5001, "Bu rol bir kullanıcıya atanmış, silinemez", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(5002, "Rol bulunamadı", HttpStatus.NOT_FOUND),
    SYSTEM_ROLE_MODIFICATION(5003, "Sistem rolleri değiştirilemez", HttpStatus.FORBIDDEN),
    DUPLICATE_ROLE_NAME(5004, "Bu isimde bir rol zaten mevcut", HttpStatus.CONFLICT),
    CANNOT_DELETE_OWN_USER(5005, "Kendi kullanıcı hesabınızı silemezsiniz", HttpStatus.BAD_REQUEST),

    // --- MEDIA & STORAGE (6000-6999) ---
    FILE_UPLOAD_ERROR(6001, "Dosya yüklenemedi", HttpStatus.INTERNAL_SERVER_ERROR),
    DIRECTORY_CREATION_ERROR(6002, "Upload dizini oluşturulamadı", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_NAME(6003, "Geçersiz dosya adı", HttpStatus.BAD_REQUEST),
    EMPTY_FILE(6004, "Dosya boş olamaz", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
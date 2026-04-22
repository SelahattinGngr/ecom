# Security Audit Report — E-Commerce Backend

**Tarih:** 2026-04-22  
**Kapsam:** Tüm kaynak kodu, konfigürasyon dosyaları, Docker ve .env dosyaları  
**Metodoloji:** Statik kod analizi

---

## Özet Tablo

| # | Başlık | Seviye | Durum |
|---|--------|--------|-------|
| S-01 | `application-dev.properties` git'te şifre açık | KRİTİK | ⏸ Kasıtlı (properties) |
| S-02 | Production profili `test` — CreateUserBean çalışıyor | KRİTİK | ⏸ Kabul edildi (dev/test) |
| S-03 | Cookie `Secure=false` hardcoded | KRİTİK | ⏸ RestClient için kasıtlı, prod'da true |
| S-04 | OTP brute-force koruması yok | YÜKSEK | ✅ Düzeltildi |
| S-05 | Actuator `mappings` ve `metrics` herkese açık | YÜKSEK | ⏸ Prod'da kaldırılacak |
| S-06 | Dosya yüklemede MIME türü doğrulaması yok | YÜKSEK | ✅ Düzeltildi |
| S-07 | X-Forwarded-For güvenilmez kaynaktan alınıyor | YÜKSEK | ✅ Düzeltildi |
| S-08 | Iyzico checkout token INFO loglanıyor | YÜKSEK | ⏸ Dev ortamı, prod'da düzeltilecek |
| S-09 | Access token süresi 7 gün | ORTA | ⏸ Kasıtlı (properties) |
| S-10 | Ödeme sağlayıcı hata mesajları istemciye iletiliyor | ORTA | ✅ Düzeltildi |
| S-11 | Redis şifresiz, port dışarı açık | ORTA | ⏸ Kabul edildi (local dev) |
| S-12 | Signup firstName/lastName max uzunluk yok | ORTA | ✅ Düzeltildi |
| S-13 | Swagger UI production'da açık | ORTA | ⏸ Frontend bitince kaldırılacak |
| S-14 | Iyzico'da sabit tarihler | ORTA | ✅ Kısmen düzeltildi (TC onaylı, tarihler güncellendi) |
| S-15 | X-Trace-Id log injection'a açık | ORTA | ✅ Düzeltildi |
| S-16 | `resend-otp` rate limiting yok | DÜŞÜK | ✅ Düzeltildi |
| S-17 | Silinen görseller diskten kaldırılmıyor | DÜŞÜK | ✅ Düzeltildi |
| S-18 | Redis `allkeys-lru` — session eviction riski | DÜŞÜK | ⏸ Not alındı |
| S-19 | `tz` parametresi doğrulanmıyor | DÜŞÜK | ✅ Düzeltildi |
| S-19 | `analytics?tz=` parametresi doğrulanmıyor | DÜŞÜK | `src/main/java/.../controller/admin/AnalyticsController.java` | 28 |

---

## KRİTİK

### S-01 — `application-dev.properties` git-tracked dosyada gerçek kimlik bilgileri

**Dosya:** `src/main/resources/application-dev.properties`  
**Satır:** 16 (Gmail şifresi), 21 (Iyzico sandbox key)

`application-dev.properties` kaynak kodun parçası olarak git ile takip edilen bir dosyadır ve içinde şunlar bulunuyor:

```properties
spring.mail.password=qzinfvsrgjykalwx     # satır 16 — Gmail App Password
selahattin.dev.jwt.access-secret-key=f2a606644100eeedd65189534b7e0d812c053525...  # satır 8
selahattin.dev.jwt.refresh-secret-key=88cc6d801a5e42c300c31cac36c673785ccbe500... # satır 9
```

`.env` gitignore'a alınmış ve doğru şekilde dışlanmış, ancak `application-dev.properties` dışlanmamış. Bu dosyayı repo'ya erişimi olan herkes görebilir.

**Etki:** Gmail hesabına yetkisiz erişim. JWT secret'ların sızdırılması token forgery'ye açık kapı açar.

**Çözüm:**
```bash
# 1. Gmail App Password'ü değiştir
# 2. Yeni JWT secret'lar üret
# 3. Git geçmişini temizle
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch src/main/resources/application-dev.properties" \
  --prune-empty --tag-name-filter cat -- --all

# 4. application-dev.properties'i .gitignore'a ekle
# 5. Her değeri ${ENV_VAR} ile yönlendir
```

---

### S-02 — Production profili `test` olarak ayarlı — `CreateUserBean` çalışıyor

**Dosya:** `src/main/resources/application.properties`  
**Satır:** 21

```properties
spring.profiles.active=test   # ← Bu satır production ortamında da test profilini aktif ediyor
```

`CreateUserBean.java:62`:
```java
@Profile({"dev", "test"})
@Component
public class CreateUserBean implements CommandLineRunner {
```

`CreateUserBean`, her uygulama başlangıcında aşağıdaki kullanıcıları oluşturuyor:

```java
saveUser("admin@example.com", "Admin", "User", "admin", true);    // satır 128
saveUser("customer1@example.com", ...)
saveUser("developer@example.com", ...)
```

Bu kullanıcılar `existsByEmail` kontrolü geçtikten sonra oluşturuluyor. OTP passwordless login sistemi olduğundan, saldırgan `admin@example.com` adresiyle OTP isteyebilir. **Eğer e-posta sunucusu bu adresi gerçek bir mailbox'a yönlendiriyorsa admin erişimi açık.**

**Etki:** Yetkisiz admin erişimi.

**Çözüm:**
```properties
# application.properties içindeki satırı kaldır veya test profilini production'dan ayır
# spring.profiles.active=test  ← bu satırı SİL
```
Docker ortamında `SPRING_PROFILES_ACTIVE=test` yerine `production` gibi ayrı bir profil kullan ve `CreateUserBean`'ı asla production profiliyle eşleştirme.

---

### S-03 — Cookie `Secure=false` hardcoded

**Dosya:** `src/main/java/selahattin/dev/ecom/utils/cookie/CookieUtil.java`  
**Satır:** 9

```java
public class CookieUtil {
    private boolean isSecure = false;   // ← hardcoded false, production'da da geçerli
```

`Secure` bayrağı olmayan cookie'ler HTTP bağlantısı üzerinden iletilebilir. Ortadaki adam (MITM) saldırısı, ağ sniffer veya yanlış yapılandırılmış reverse proxy ile JWT token'ları çalınabilir.

**Etki:** Access token ve refresh token sızıntısı. Session hijacking.

**Çözüm:**
```java
// application.properties'e ekle
app.cookie.secure=${COOKIE_SECURE:true}  # production'da true, dev'de false

// CookieUtil.java
@Value("${app.cookie.secure:true}")
private boolean isSecure;
```

**Not (2026-04-22):** `Secure=false` değeri RestClient ile HTTP üzerinden test yapılırken çerezin iletilebilmesi için kasıtlı olarak bu şekilde bırakılmıştır. Production'a geçişte `true` olarak değiştirilmesi gerekmektedir.

---

## YÜKSEK

### S-04 — OTP brute-force koruması yok

**Dosya:** `src/main/java/selahattin/dev/ecom/service/domain/AuthService.java`  
**Satır:** 61-68 (`validateRedisValue`), 36 (`generateOtp`)

6 haneli numerik OTP = 1.000.000 kombinasyon. Hatalı giriş Redis key'ini **silmiyor**, bu yüzden aynı OTP'ye dakikalarca tekrar denemeliyapılabilir:

```java
private void validateRedisValue(String key, String expectedValue) {
    Object actualValue = redisTemplate.opsForValue().get(key);
    if (actualValue == null || !actualValue.toString().equals(expectedValue)) {
        throw new BusinessException(ErrorCode.INVALID_OTP);
        // ← Hatalı deneme sayacı artırılmıyor, key silinmiyor
    }
}
```

5 dakikalık TTL içinde sınırsız deneme yapılabilir. 300 saniye × (örn. 10 req/sn) = 3.000 deneme. 10.000 istek ile %1 başarı ihtimali var.

**Etki:** Hesap ele geçirme.

**Çözüm:**
```java
// Redis'te deneme sayacı tut
String attemptsKey = "auth:otp_attempts:" + email;
Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
redisTemplate.expire(attemptsKey, OTP_DURATION_MINUTES, TimeUnit.MINUTES);

if (attempts > MAX_OTP_ATTEMPTS) {  // MAX_OTP_ATTEMPTS = 5
    deleteFromRedis(OTP_KEY_TEMPLATE + email);
    throw new BusinessException(ErrorCode.INVALID_OTP, "Çok fazla hatalı deneme.");
}
```

**✅ Düzeltme (2026-04-22):** `AuthService.verifyLoginOtp` metoduna Redis tabanlı deneme sayacı eklendi. Her hatalı denemede `auth:otp_attempts:{email}` key'i artırılıyor, TTL OTP süresiyle eşleniyor. 5 hatalı denemeden sonra OTP key'i siliniyor ve kullanıcı kilitlenip yeni OTP talep etmek zorunda kalıyor. Başarılı girişte sayaç temizleniyor. Sabitlenen değerler `AuthConstant.java`'ya (`MAX_OTP_ATTEMPTS = 5`) taşındı.

---

### S-05 — Actuator `mappings` ve `metrics` herkese açık

**Dosya 1:** `src/main/resources/application.properties` satır 9  
**Dosya 2:** `src/main/java/selahattin/dev/ecom/config/SecurityConfig.java` satır 65

```properties
management.endpoints.web.exposure.include=health,info,metrics,mappings  # ← mappings açığa çıkıyor
```

```java
.requestMatchers("/actuator/**").permitAll()  // ← kimlik doğrulaması yok
```

`/actuator/mappings` tüm endpoint'leri, handler'ları ve controller eşlemelerini açık ediyor. `/actuator/metrics` JVM bellek kullanımı, thread sayısı, DB bağlantı havuzu boyutunu açık ediyor.

**Etki:** Saldırgan tüm API haritasını ve iç metrikleri elde edebilir.

**Çözüm:**
```properties
# Sadece health açık bırak
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```

---

### S-06 — Dosya yüklemede MIME türü ve içerik doğrulaması yok

**Dosya:** `src/main/java/selahattin/dev/ecom/service/infra/FileStorageService.java`  
**Satır:** 30-55

```java
public String save(MultipartFile file) {
    // Sadece ".." path traversal kontrolü var
    if (originalFileName.contains("..")) { ... }
    
    // ← MIME türü doğrulaması YOK
    // ← Dosya uzantısı doğrulaması YOK
    // ← İçerik sihirli bayt (magic byte) doğrulaması YOK
    
    String fileName = UUID.randomUUID() + "_" + originalFileName;
    Files.copy(file.getInputStream(), rootLocation.resolve(fileName), ...);
}
```

Yüklenen dosyalar `/assets/public/products/` üzerinden herkese açık şekilde servis ediliyor. Saldırgan şunları yükleyebilir:
- `.html` / `.svg` → XSS payload içerebilir, tarayıcı aynı origin'den çalıştırır
- `.jar` → deploy context'e göre kod çalıştırma riski
- 2 MB sınırlı ancak içerik sınırı yok

**Etki:** Stored XSS, veri bütünlüğü ihlali.

**Çözüm:**
```java
private static final List<String> ALLOWED_MIME_TYPES = List.of(
    "image/jpeg", "image/png", "image/webp", "image/svg+xml", "image/gif"
);

private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

public String save(MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
        throw new BusinessException(ErrorCode.INVALID_FILE_NAME, "Desteklenmeyen dosya türü.");
    }
    // Magic byte doğrulaması için Apache Tika eklenebilir
}
```

**✅ Düzeltme (2026-04-22):** `FileStorageService.save()` metoduna MIME tipi (`image/jpeg`, `image/png`, `image/webp`, `image/gif`) ve dosya uzantısı (`.jpg`, `.jpeg`, `.png`, `.webp`, `.gif`) çift doğrulaması eklendi. Desteklenmeyen türler `INVALID_FILE_NAME` hatasıyla reddediliyor. `FileStorageService` catch bloğundaki ham exception mesajı da kaldırıldı.

---

### S-07 — X-Forwarded-For güvenilmez kaynaktan alınıyor

**Dosyalar:**  
- `src/main/java/.../service/domain/AuthService.java` satır 233-240  
- `src/main/java/.../controller/PaymentController.java` satır 49-57  
- `src/main/java/.../config/filter/MdcLoggingFilter.java` satır 60-66

```java
private String getClientIp(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    if (xfHeader == null) {
        return request.getRemoteAddr();
    }
    return xfHeader.split(",")[0];  // ← istemci bu header'ı serbestçe sahte gönderebilir
}
```

`X-Forwarded-For` header'ı her istemci tarafından serbestçe gönderilip manipüle edilebilir. Trusted proxy listesi olmadan bu IP'ye güvenilemez.

**Etki:** IP tabanlı güvenlik kayıtları (güvenlik log'ları, audit trail) yanıltıcı olur. Iyzico entegrasyonuna iletilen IP saldırgan tarafından kontrol edilebilir.

**Çözüm:**  
Reverse proxy olarak Nginx veya traefik kullanılıyorsa, uygulama sunucusuna ulaşan `X-Forwarded-For`'un sadece güvenilir proxy'den geldiğini garanti et. Spring'de bunu `server.forward-headers-strategy=framework` veya `ForwardedHeaderFilter` ile yapılandır.

**✅ Düzeltme (2026-04-22):** `X-Forwarded-For` trust kaldırıldı. `AuthService.getClientIp()`, `MdcLoggingFilter.extractClientIp()` ve `PaymentController.extractClientIp()` metotlarının tamamı doğrudan `request.getRemoteAddr()` kullanacak şekilde güncellendi. Production'da reverse proxy yapılandırılınca `server.forward-headers-strategy=framework` eklenerek Spring'in güvenli XFF desteğine geçilecek.

---

### S-08 — Iyzico checkout token INFO düzeyinde loglanıyor

**Dosya:** `src/main/java/.../service/integration/payment/impl/IyzicoPaymentProvider.java`  
**Satır:** 164, 173

```java
log.info("[IYZICO] Ödeme sonucu sorgulanıyor. Token: {}", token);   // satır 164
log.info("[IYZICO] Ödeme BAŞARILI. Token: {}", token);               // satır 173
```

Iyzico checkout form token'ı, ödeme işlemini temsil eden hassas bir tanımlayıcıdır. Log dosyaları genellikle daha geniş erişime sahiptir; bu token log'larda açık şekilde görünür.

**Etki:** Log erişimi olan kişi token'ı kullanarak işlem detaylarını sorgulayabilir.

**Çözüm:**
```java
// Token'ı maskele veya sadece ilk/son 4 karakterini logla
log.info("[IYZICO] Ödeme sorgulanıyor. Token: {}...{}", 
    token.substring(0, 4), 
    token.substring(token.length() - 4));
```

**Not (2026-04-22):** Geliştirme ortamı olduğu için şimdilik bırakılmıştır. Production'a geçişte token maskelenmeli veya log düzeyi DEBUG'a düşürülmelidir.

---

## ORTA

### S-09 — Access token süresi 7 gün

**Dosya:** `.env`  
**Satır:** 11

```env
JWT_ACCESS_TOKEN_EXPIRATION_MS=604800000   # = 7 gün
```

Access token kısa ömürlü olmalıdır (standart: 15-60 dakika). 7 günlük access token çalınırsa, Redis session'ı silinmedikçe 7 gün boyunca geçerli kalır.

Not: Sistem Redis session'ı kontrol ettiğinden bu kısmen hafifletilmiş. Ancak Redis session'ı var ve access token çalınmışsa, kullanıcı oturumu kapatana kadar saldırgan erişim sürdürebilir.

**Çözüm:**
```env
JWT_ACCESS_TOKEN_EXPIRATION_MS=900000   # = 15 dakika
JWT_REFRESH_TOKEN_EXPIRATION_MS=604800000  # = 7 gün (refresh token uzun olabilir)
```

---

### S-10 — Ödeme sağlayıcı hata mesajları istemciye iletiliyor

**Dosyalar:**  
- `src/main/java/.../integration/payment/impl/StripePaymentProvider.java` satır 104, 131, 165  
- `src/main/java/.../integration/payment/impl/IyzicoPaymentProvider.java` satır 230, 267  
- `src/main/java/.../service/infra/FileStorageService.java` satır 55

```java
throw new BusinessException(ErrorCode.PAYMENT_INIT_ERROR, "Stripe hatası: " + e.getMessage());
throw new BusinessException(ErrorCode.PAYMENT_FAILED, "Iyzico iade hatası: " + e.getMessage());
throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "Hata detayı: " + e.getMessage());
```

`BusinessException` mesajı `GlobalExceptionHandler` tarafından `ApiResponse.error(ex.getMessage(), ...)` içinde **doğrudan istemciye** gönderiliyor. Stripe/Iyzico SDK hataları, iç yapı veya konfigürasyon bilgisi içerebilir.

**Çözüm:**
```java
// İç hatayı logla, istemciye genel mesaj ver
log.error("[STRIPE] Capture hatası. PaymentId: {}", payment.getId(), e);
throw new BusinessException(ErrorCode.PAYMENT_FAILED, "Ödeme işlemi gerçekleştirilemedi.");
```

**✅ Düzeltme (2026-04-22):** Tüm `catch` bloklarında ham exception/SDK mesajı `BusinessException`'a geçirilmesi kaldırıldı. Detaylı hata `log.error()` ile loglanıyor, istemciye yalnızca genel mesaj dönüyor. Etkilenen yöntemler: `StripePaymentProvider.initializePayment`, `voidPayment`, `refundPayment` ve `IyzicoPaymentProvider.initializePayment`, `voidPayment`, `refundPayment` (6 lokasyon).

---

### S-11 — Redis şifresiz çalışıyor, host portunun dışarıya açıldığı kontrol edilmeli

**Dosya:** `docker-compose.yaml` satır 40-60

```yaml
redis:
  ports:
    - "6379:6379"   # ← host'un 6379 portunu açık bırakıyor
  command: redis-server --maxmemory 128mb --maxmemory-policy allkeys-lru
  # ← --requirepass yok
```

Redis şifresiz çalışıyor. Port 6379 host'a eşlendiğinden, sunucunun güvenlik duvarı yanlış yapılandırılmışsa Redis'e ağdan doğrudan erişilebilir. OTP'ler, session'lar ve email kuyruğu okunabilir/değiştirilebilir.

**Çözüm:**
```yaml
redis:
  command: redis-server --requirepass ${REDIS_PASSWORD} --maxmemory 128mb ...
  ports:
    - "127.0.0.1:6379:6379"  # sadece localhost'a bağla
```

```properties
spring.data.redis.password=${REDIS_PASSWORD}
```

---

### S-12 — Signup request'te firstName/lastName max uzunluk doğrulaması yok

**Dosya:** `src/main/java/.../dto/request/SignupRequest.java`

```java
@NotBlank(message = "İsim Boş Bırakılamaz")
private String firstName;   // ← @Size yok, maksimum uzunluk kısıtı yok

@NotBlank(message = "Soyisim Boş Bırakılamaz")
private String lastName;
```

Çok uzun değerler veritabanı kolon sınırını aşarak hata üretebilir veya log dosyalarını doldurmak için kullanılabilir.

**Çözüm:**
```java
@Size(max = 100, message = "İsim en fazla 100 karakter olabilir")
private String firstName;

@Size(max = 100, message = "Soyisim en fazla 100 karakter olabilir")
private String lastName;
```

**✅ Düzeltme (2026-04-22):** `SignupRequest.java`'ya `@Size(max = 100)` anotasyonu `firstName` ve `lastName` alanlarına eklendi.

---

### S-13 — Swagger UI production'da herkese açık

**Dosya:** `src/main/java/.../config/SecurityConfig.java` satır 67

```java
.requestMatchers("/v3/api-docs/**").permitAll()
.requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
```

Tüm API şeması, endpoint listesi, request/response modelleri kimlik doğrulaması olmadan erişilebilir.

**Çözüm:**
```java
// Production profili için Swagger'ı devre dışı bırak
// application-test.properties:
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true

// application.properties (production):
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

---

### S-14 — Iyzico entegrasyonunda sahte T.C. kimlik numarası

**Dosya:** `src/main/java/.../integration/payment/impl/IyzicoPaymentProvider.java`  
**Satır:** 89-92

```java
buyer.setIdentityNumber("11111111111");    // ← sahte TC
buyer.setLastLoginDate("2024-01-01 12:00:00");   // ← sabit tarih
buyer.setRegistrationDate("2024-01-01 12:00:00"); // ← sabit tarih
```

Iyzico production ortamında doğru T.C. kimlik numarası ve gerçek tarihler gerektirir. Bu değerlerle production'a geçilirse işlemler reddedilebilir veya Iyzico hesabı askıya alınabilir.

**Çözüm:** Kullanıcı profilinde `identityNumber` alanı ekle veya Iyzico'nun izin verdiği dummy değerlerini belgele. `lastLoginDate` ve `registrationDate` gerçek kullanıcı tarihlerinden türetilmeli.

**Güncelleme (2026-04-22):** Iyzico ile teyit edilmiştir — TC kimlik numarası için `11111111111` değeri kabul edilmektedir ve değiştirilmeyecektir. `lastLoginDate` artık `OffsetDateTime.now()`, `registrationDate` ise `user.getCreatedAt()` kullanılarak gerçek veriyle doldurulmaktadır.

---

### S-15 — X-Trace-Id header'ı log injection'a açık

**Dosya:** `src/main/java/.../config/filter/MdcLoggingFilter.java`  
**Satır:** 35-40

```java
String traceId = request.getHeader("X-Trace-Id");
if (traceId == null || traceId.isEmpty()) {
    traceId = UUID.randomUUID().toString()...;
}
MDC.put(TRACE_ID_KEY, traceId);  // ← istemciden gelen değer sanitize edilmeden MDC'ye giriyor
```

İstemci `X-Trace-Id: \n[FAKE LOG ENTRY] Admin login successful` gibi değer gönderebilir. Logback konfigürasyonuna göre bu, log dosyasında sahte kayıt üretebilir.

**Açıklama:** Log dosyaları güvenlik olaylarını izlemek için kullanılır. Saldırgan `X-Trace-Id` header'ına `\n` (yeni satır) karakteri koyarak log dosyasına sahte satır ekleyebilir. Örneğin: `X-Trace-Id: abc\n[ERROR] Admin login from 127.0.0.1 — SUCCESS` gönderilirse, log dosyasında bu ikinci satır gerçek bir güvenlik olayı gibi görünür. Bu durum güvenlik denetimini (security audit) yanıltabilir. Çözüm: header değerini yalnızca harf, rakam ve tire karakterlerine izin verecek şekilde filtrele.

**Çözüm:**
```java
String traceId = request.getHeader("X-Trace-Id");
if (traceId != null && traceId.matches("[a-zA-Z0-9\\-]{1,64}")) {
    MDC.put(TRACE_ID_KEY, traceId);
} else {
    MDC.put(TRACE_ID_KEY, UUID.randomUUID().toString().replace("-", "").substring(0, 16));
}
```

**✅ Düzeltme (2026-04-22):** `MdcLoggingFilter.doFilterInternal()` metodunda `X-Trace-Id` header değeri `[a-zA-Z0-9\-]{1,64}` regex'iyle doğrulanıyor. Bu kalıba uymayan değerler (yeni satır, özel karakter, çok uzun string) reddedilip yerine UUID üretiliyor. Response'a da temizlenmiş değer yazılıyor.

---

## DÜŞÜK

### S-16 — `resend-otp` ve `resend-verification-email` rate limiting yok

**Dosya:** `src/main/java/.../controller/AuthController.java`  
**Satır:** 38, 56

```java
@PostMapping("/public/resend-otp")
public ResponseEntity<...> resendLoginOtp(@Valid @RequestBody SigninRequest signinRequest) {
    authService.sendLoginOtp(signinRequest);  // ← sınırsız OTP talebi yapılabilir
```

Aynı email adresine dakikada binlerce OTP maili gönderilmesine zemin hazırlar. Email spam aracı olarak kullanılabilir.

**Çözüm:** Redis tabanlı rate limiter ekle:
```java
String rateLimitKey = "auth:otprate:" + email;
Long count = redisTemplate.opsForValue().increment(rateLimitKey);
if (count == 1) redisTemplate.expire(rateLimitKey, 1, TimeUnit.MINUTES);
if (count > 3) throw new BusinessException(ErrorCode.INVALID_REQUEST, "Çok fazla deneme. 1 dakika bekleyin.");
```

**✅ Düzeltme (2026-04-22):** `AuthService`'e `checkOtpRateLimit(email)` yardımcı metodu eklendi. `sendLoginOtp()` ve `resendVerificationEmail()` metodlarının başında çağrılıyor. `auth:otprate:{email}` key'i Redis'te dakikada 3 isteği sayıyor, aşıldığında `INVALID_REQUEST` hatası fırlatıyor. Sabitler `AuthConstant.java`'ya (`MAX_OTP_RATE_PER_MINUTE = 3`) taşındı.

---

### S-17 — Silinen görseller diskten kaldırılmıyor

**Dosya:** `src/main/java/.../service/domain/admin/AdminProductsService.java`  
**Satır:** 212-220

```java
public void deleteImage(UUID productId, UUID imageId) {
    ...
    image.setDeletedAt(OffsetDateTime.now());   // ← soft delete
    imageRepository.save(image);
    // ← fiziksel dosya assets/public/products/ altında kalıyor
    // ← URL bilinirse dosya hâlâ erişilebilir
}
```

**Etki:** Disk dolma riski, "silindi" sanılan içeriğin erişilebilir kalması.

**Çözüm:** `FileStorageService.delete(url)` metodu ekle ve `deleteImage` içinden çağır.

**✅ Düzeltme (2026-04-22):** `FileStorageService`'e `delete(String fileUrl)` metodu eklendi. `AdminProductsService.deleteImage()` soft delete işlemi öncesinde `fileStorageService.delete(image.getUrl())` çağırıyor. Fiziksel silme başarısız olursa yalnızca uyarı loglanıyor, soft delete işlemi iptal edilmiyor.

---

### S-18 — Redis `allkeys-lru` policy — session eviction riski

**Dosya:** `docker-compose.yaml` satır 49

```yaml
command: redis-server --maxmemory 128mb --maxmemory-policy allkeys-lru
```

`allkeys-lru` ile Redis belleği dolunca **TTL olan/olmayan tüm key'ler** dahil session'lar ve OTP'ler silinebilir. Bu beklenmedik oturum sonlanmalarına ve doğrulama kodlarının kaybolmasına yol açar.

**Çözüm:**
```yaml
command: redis-server --maxmemory 128mb --maxmemory-policy volatile-lru
```

`volatile-lru` sadece TTL set edilmiş key'leri siler. Session ve OTP key'lerine TTL uygulandığı için bu kısıtlama onları da etkileyecek, ancak TTL süresi dolmadan rasgele silinmesi engellenmiş olacak.

**Açıklama — allkeys-lru vs volatile-lru Farkı:**

Redis'e 128MB limit konulduğunda bu limit dolunca Redis yer açmak için key silmek zorunda kalır. `allkeys-lru` politikası "En az kullanılanı sil, TTL'i olan veya olmayan fark etmez" anlamına gelir.

Örnek senaryo:
1. Redis doldu
2. Kullanıcının aktif session key'i (`auth:rt:{jti}`) uzun süredir Redis tarafından "kullanılmadı" olarak işaretlendi
3. Redis onu siliyor
4. Kullanıcı hiçbir şey yapmadan oturumdan düşüyor

`volatile-lru` kullanıldığında sadece TTL'i olan key'ler silinmeye aday olur — en azından "hepsini sil" yerine daha seçici davranır.

**Not:** 128MB local dev için yeterli olduğundan bu durum pratikte tetiklenmez. Production'da Redis belleği daha büyük tutulursa hiç sorun olmaz. Düşük risk, ancak production'a geçişte `volatile-lru` olarak değiştirilmesi önerilir.

---

### S-19 — Analytics `tz` parametresi doğrulanmıyor

**Dosya:** `src/main/java/.../controller/admin/AnalyticsController.java` satır 28  
**İlgili:** `src/main/java/.../repository/order/OrderRepository.java` satır 58-67

```sql
SELECT TO_CHAR(DATE_TRUNC('day', created_at AT TIME ZONE :tz), ...) FROM orders
```

`:tz` parametresi JDBC prepared statement olarak bağlanıyor, bu yüzden SQL injection **mümkün değil**. Ancak geçersiz timezone değeri (`America/Invalid`, `';DROP TABLE--`) PostgreSQL'in hata fırlatmasına yol açar ve istemci 500 alır.

**Açıklama:** `tz` parametresi `America/New_York` veya `Europe/Istanbul` gibi geçerli bir timezone adı olmalıdır. Geçersiz değer girildiğinde PostgreSQL `ERROR: time zone "xxx" not recognized` hatası fırlatır ve Spring bunu 500 Internal Server Error olarak istemciye döner. Doğru davranış: önce Java'da `ZoneId.of(tz)` ile doğrulayıp geçersizse 400 Bad Request dönmek. SQL injection riski yok çünkü prepared statement kullanılıyor.

**Çözüm:**
```java
// Servis katmanında doğrula
try {
    ZoneId.of(tz);
} catch (DateTimeException e) {
    throw new BusinessException(ErrorCode.INVALID_REQUEST, "Geçersiz timezone: " + tz);
}
```

**✅ Düzeltme (2026-04-22):** `AnalyticsService`'e `validateTimezone(String tz)` özel metodu eklendi. `getDashboardAnalytics`, `getOrderAnalytics` ve `getUserAnalytics` metodlarının başında çağrılıyor (bu üç metot `tz` parametresini gerçekten SQL'e iletiyor). Geçersiz timezone değeri PostgreSQL'e ulaşmadan `INVALID_REQUEST` (400) fırlatıyor.

---

## Ek Notlar

### .env Dosyası — Plaintext Şifreler

`.env` dosyası gitignore'da ve commit edilmiyor, bu doğru. Ancak sunucu üzerinde plaintext olarak duruyor:
```
POSTGRES_DB_PASSWORD=!Sg27.09.2000   # ← kişisel bilgi içerir (doğum tarihi?)
GMAIL_PASSWORD=zvecdeybgtbopvbt
```

Üretim ortamında `secrets management` (AWS Secrets Manager, HashiCorp Vault, Docker Secrets) kullanılması önerilir.

### JWT Key Kullanımı

`JwtTokenProvider.java:28`:
```java
Keys.hmacShaKeyFor(jwtProperties.getAccessSecretKey().getBytes(StandardCharsets.UTF_8))
```

Secret key 64 karakter hex string olarak `getBytes(UTF-8)` ile alınıyor — yani 64 byte'lık ASCII, 256 bit entropi. Minimum 256 bit gereksinimi karşılanıyor. Ancak rastgele byte üreteci (örn. `SecureRandom`) ile üretilmiş key daha güvenli olurdu.

### `dev/CreateUserBean.java` — Test Verisi Güvenlik Notu

`admin@example.com` kullanıcısı `example.com` domainine aittir. `example.com` IANA tarafından test amaçlı ayrılmış bir domain olup kimseye ait değil — email ulaşmaz. Bu yüzden OTP ile bu hesaba giriş yapılamaz. Ancak aktif SMTP konfigürasyonu mail gönderilmesini engellemez ve bu bir kaynak israfıdır.

### Redis Bellek Boyutlandırması (S-18 Ek Not)

**Dev ortamında 128MB ne zaman dolar?**

Mevcut test verisiyle Redis kullanımı tahmini:

| Key Tipi | Boyut/adet | Dev'de tahmini adet | Toplam |
|----------|-----------|-------------------|--------|
| `auth:rt:{jti}` (session) | ~700 byte | 5–10 | ~7 KB |
| `auth:signin_otp:{email}` | ~100 byte | 0–5 | <1 KB |
| `prd:slug:{slug}` (ürün cache) | ~2.5 KB | 10–50 | ~125 KB |
| `cat:tree` | ~30 KB | 1 | 30 KB |
| `prd:list:{hash}` (liste cache) | ~8 KB | 5–10 | ~80 KB |
| `site:public:config` | ~5 KB | 1 | 5 KB |
| `security:roles:{role}` | ~500 byte | 3–5 | ~2 KB |

**Dev toplam: ~250 KB** — 128 MB'ın %0.2'si. Pratikte dolmaz.

**Production'da 128 MB ne kadar taşır?**

En çok yer tüketen bileşen session'lardır:
```
128 MB / ~750 byte per session ≈ 178.000 eş zamanlı aktif oturum
```
Ürün cache için:
```
128 MB / ~2.5 KB per product ≈ 52.000 ürün
```

| Kullanıcı ölçeği | Önerilen Redis belleği |
|-----------------|----------------------|
| <10K aktif kullanıcı | 128 MB yeter |
| 10K–100K aktif kullanıcı | 256 MB–512 MB |
| 100K+ aktif kullanıcı | 1 GB+ |

**Asıl risk:** `prd:list:{queryHash}` — her farklı filtre/sayfa/sıralama kombinasyonu ayrı key açıyor. TTL 60 saniye olduğu için hızlı döner ama yoğun trafik altında anlık olarak en çok yer kaplayan key grubu bu olabilir.

---

*Bu rapor yalnızca statik analiz içermektedir. Dinamik test (penetrasyon testi, fuzzing) yapılmamıştır.*

# Üretim Log Hata Raporu — 2026-05-20

Kaynak: `ecom-backend.log` (uzak sunucu, 2 MB)  
Analiz tarihi: 2026-05-20

---

## H-1 — SiteConfigResponse Jackson Deserialize Hatası

**Seviye:** WARN (çok tekrarlı — yüzlerce kez)  
**Hata:**
```
InvalidDefinitionException: Cannot construct instance of
`selahattin.dev.ecom.dto.response.site.SiteConfigResponse`
(no Creators, like default constructor, exist)
```

**Neden:** `SiteConfigResponse`, `AssetSlotResponse` ve `AssetResponse` DTO sınıfları yalnızca `@Getter @Builder` annotasyonlarına sahipti. Lombok `@Builder` no-args constructor üretmez. Jackson nesneleri oluştururken no-args constructor gerektirir.

**Etki:** `site:public:config` Redis cache HİÇ okunamıyordu. Her istek için DB'ye fallback yapılıyordu. Cache'in faydası sıfıra düşüyordu.

**İlgili Dosyalar ve Satırlar:**
| Dosya | Satır | Sorun |
|-------|-------|-------|
| `src/main/java/selahattin/dev/ecom/dto/response/site/SiteConfigResponse.java` | 8–10 | `@Getter @Builder` → no-args constructor yok |
| `src/main/java/selahattin/dev/ecom/dto/response/site/AssetSlotResponse.java` | 6–8 | `@Getter @Builder` → no-args constructor yok |
| `src/main/java/selahattin/dev/ecom/dto/response/site/AssetResponse.java` | 9–11 | `@Getter @Builder` → no-args constructor yok |
| `src/main/java/selahattin/dev/ecom/service/domain/SiteConfigService.java` | 48 | `objectMapper.readValue(cached, SiteConfigResponse.class)` başarısız |

**Çözüm (uygulandı):** Her üç DTO sınıfına `@NoArgsConstructor` + `@AllArgsConstructor` eklendi, `@Getter` → `@Data` olarak değiştirildi.

---

## H-2 — CategoryService Redis SerializationException (KRİTİK)

**Seviye:** ERROR (tekrarlı)  
**Hata:**
```
SerializationException: Could not read JSON:
Unexpected token (`JsonToken.START_OBJECT`), expected `JsonToken.VALUE_STRING`:
need String, Number of Boolean value that contains type id
(for subtype of java.lang.Object)
```

**Stack trace:** `CategoryService.getAllCategories(CategoryService.java:33)` → `GlobalExceptionHandler`

**Neden:** `CategoryService` `RedisTemplate<String, Object>` kullanıyordu. Bu template `GenericJacksonJsonRedisSerializer` ile tip bilgisi (type-info) sarmalı format (`["java.util.ArrayList",[...]]`) yazar/okur. Redis'teki `cat:tree` değeri farklı bir deployment veya format versiyonundan kalmış plain JSON idi. Jackson 3.x (Spring Boot 4.0) bu formatı okuyamadı. Ek olarak `CategoryResponse` da `@NoArgsConstructor` içermiyordu.

**Etki:** `GET /api/v1/categories` endpointi her çağrıda 500 Internal Server Error döndürüyordu. **Kritik endpoint tamamen kırıktı.**

**İlgili Dosyalar ve Satırlar:**
| Dosya | Satır | Sorun |
|-------|-------|-------|
| `src/main/java/selahattin/dev/ecom/service/domain/CategoryService.java` | 28–33 | `RedisTemplate<String,Object>` kullanımı, deserialize başarısız |
| `src/main/java/selahattin/dev/ecom/dto/response/catalog/CategoryResponse.java` | 7–9 | `@Getter @Builder` → no-args constructor yok |
| `src/main/java/selahattin/dev/ecom/service/domain/admin/AdminCategoryService.java` | 29 | `RedisTemplate<String,Object>` ile `delete()` — tutarsız bağımlılık |

**Çözüm (uygulandı):**
1. `CategoryResponse`'a `@NoArgsConstructor` + `@AllArgsConstructor` eklendi.
2. `CategoryService` ve `AdminCategoryService`, `StringRedisTemplate` + `ObjectMapper` desenine geçirildi (`SiteConfigService` ile aynı pattern). `GenericJacksonJsonRedisSerializer`'ın type-info sarmalı formatı tamamen devre dışı bırakıldı.

> **Sunucu aksiyon:** Deploy sonrası `redis-cli DEL cat:tree` komutu çalıştırılmalı (eski bozuk veri temizlensin).

---

## H-3 — Gmail SMTP Kimlik Doğrulama Hatası

**Seviye:** WARN  
**Hata:**
```
MailHealthIndicator: Mail health check failed
jakarta.mail.AuthenticationFailedException
Caused by: jakarta.mail.MessagingException: Exception reading response
Caused by: java.net.SocketTimeoutException: Read timed out
```

**Neden:** Gmail SMTP kimlik doğrulaması başarısız. İki olası neden:
1. Gmail uygulama şifresi (App Password) süresi dolmuş veya yanlış yapılandırılmış.
2. `spring.mail.properties.mail.smtp.timeout=3000` (3 saniye) SMTP handshake için çok kısa.

**Etki:** `actuator/health` mail bileşenini DOWN gösteriyor. Gerçek e-posta gönderimi de başarısız olabilir.

**İlgili Dosya ve Satır:**
| Dosya | Satır | Sorun |
|-------|-------|-------|
| `src/main/resources/application-prod.properties` | 27–28 | `smtp.timeout=3000` çok kısa |

**Çözüm (uygulandı — kod tarafı):** `spring.mail.properties.mail.smtp.timeout` değeri `3000` → `10000` olarak artırıldı.

> **Altyapı aksiyon (gerekli):** Gmail hesabında "Uygulama Şifresi" (App Password) yenilenmelidir. 2FA aktifse normal şifre çalışmaz.

---

## H-4 — HikariCP Bağlantı Validasyon Hatası

**Seviye:** WARN  
**Hata:**
```
HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@31d77301
(This connection has been closed.). Possibly consider using a shorter maxLifetime value.
```

**Neden:** `application-prod.properties`'de HikariCP `maxLifetime` tanımlı değildi. Spring Boot varsayılanı 30 dakika. PostgreSQL sunucusu uzun süre bekleyen bağlantıları daha erken kapatıyor. Hikari açık zannettiği bağlantıyı kullanmaya çalışınca geçersiz buldu.

**Etki:** Otomatik yeni bağlantı alındı (recovery var), ama yük altında gecikmeye yol açabilir.

**İlgili Dosya:**
| Dosya | Satır | Sorun |
|-------|-------|-------|
| `src/main/resources/application-prod.properties` | — | HikariCP ayarları eksik |

**Çözüm (uygulandı):** Aşağıdaki ayarlar eklendi:
```properties
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.keepalive-time=60000
```

---

## H-5 — HikariCP Thread Starvation / Clock Leap

**Seviye:** WARN  
**Hata:**
```
HikariPool-1 - Thread starvation or clock leap detected
(housekeeper delta=1m46s306ms320µs201ns).
```

**Neden:** JVM yaklaşık 1 dakika 46 saniye boyunca duraksamış (Stop-the-World GC veya Docker container kaynak baskısı). Hikari housekeeping thread bu sürede çalışamadığından büyük zaman atlaması tespit etti.

**Etki:** WARN, Hikari kendiliğinden toparlar. Kullanıcı görünür hata almaz.

**Çözüm:** Kod değişikliği gerekmez. **Altyapı aksiyonu:**
- Docker container bellek limitini artır
- JVM heap ayarla: `-Xmx` ve `-Xms` dengele
- GC algoritmasını değerlendirin: G1GC (varsayılan) yerine ZGC daha kısa duraklamalar sağlar

---

## H-6 — Connection Reset by Peer (Gürültü)

**Seviye:** ERROR (yanıltıcı — sunucu hatası değil)  
**Hata:**
```
HttpMessageNotWritableException: Could not write JSON:
ServletOutputStream failed to flush: java.io.IOException: Connection reset by peer
Caused by: ClientAbortException: java.io.IOException: Connection reset by peer
```

**Neden:** İstemci (browser/mobil uygulama) yanıt tamamlanmadan bağlantıyı kapattı. Bu normal bir istemci davranışı (sekmeyi kapattı, gezinmeyi iptal etti vb.). `ClientAbortException` → `HttpMessageNotWritableException` zinciri `GlobalExceptionHandler.handleGenericException()` tarafından ERROR olarak loglanıyordu.

**Etki:** Sunucu hatası değil, hatalı sınıflandırılmış log. ERROR log gürültüsü oluşturuyordu.

**İlgili Dosya ve Satır:**
| Dosya | Satır | Sorun |
|-------|-------|-------|
| `src/main/java/selahattin/dev/ecom/exception/GlobalExceptionHandler.java` | 86–94 | `handleGenericException` `ClientAbortException`'ı ERROR logluyor |

**Çözüm (uygulandı):** `HttpMessageNotWritableException` için özel handler eklendi. Cause chain'de `ClientAbortException` veya `Connection reset` içeren `IOException` tespit edildiğinde DEBUG seviyesinde loglanır, gereksiz ERROR log üretilmez.

---

## Özet

| # | Hata | Seviye | Etki | Durum |
|---|------|--------|------|-------|
| H-1 | SiteConfigResponse deserialize | WARN | Cache çalışmıyor, her istek DB'ye düşüyor | ✅ Düzeltildi |
| H-2 | CategoryService 500 hatası | ERROR | `/api/v1/categories` tamamen kırık | ✅ Düzeltildi |
| H-3 | Gmail SMTP auth hatası | WARN | Mail gönderimi başarısız | ✅ Timeout artırıldı / Altyapı aksiyon gerekli |
| H-4 | HikariCP stale connection | WARN | Performans kaybı riski | ✅ Düzeltildi |
| H-5 | HikariCP clock leap | WARN | Otomatik toparlanıyor | ⚙️ Altyapı aksiyonu gerekli |
| H-6 | Connection reset ERROR log | ERROR | Log gürültüsü | ✅ Düzeltildi |

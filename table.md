# Production Hazırlık ve Altyapı Eksikleri

---

## 1. Production Hazırlık Değerlendirmesi

### Güvenlik — 6/10

| Madde | Durum |
|-------|-------|
| JWT HttpOnly cookie | ✅ |
| RBAC + permission sistemi | ✅ |
| OTP brute-force + rate limiting | ✅ |
| Stripe webhook imza doğrulama | ✅ |
| Audit + security event loglama | ✅ |
| Cookie `Secure=false` | ❌ prod'da true yapılmalı |
| Actuator herkese açık | ❌ kapatılmalı |
| Swagger herkese açık | ❌ kapatılmalı |
| Secrets git'te | ❌ `.gitignore` + env yönetimi |
| Redis şifresiz | ❌ prod'da requirepass |

### Altyapı — 5/10

| Madde | Durum |
|-------|-------|
| Flyway migrations | ✅ |
| Redis caching stratejisi | ✅ |
| Docker Compose mevcut | ✅ |
| PostgreSQL + Redis şifresiz | ❌ |
| Secrets management yok | ❌ |
| Redis `allkeys-lru` riski | ❌ `volatile-lru` olmalı |
| HTTPS / SSL | ❌ Nginx + cert-manager yok |
| Health check / readiness probe | ❌ |

### İşlevsellik — 9/10

Tüm core domain'ler (auth, catalog, order, payment, analytics, refund, site-config) tamamlanmış. Stripe + Iyzico çalışıyor. Return/refund flow var. Eksik olan sadece frontend.

### Kod Kalitesi — 8/10

Mimari tutarlı, hata yönetimi standart, MapStruct kullanılıyor, N+1 önlemleri alınmış. Email şu an plain text — HTML template'e geçilebilir ama blocker değil.

**Genel skor: ~7/10**

Kod tarafı production'a yakın. Asıl eksik altyapı ve konfigürasyon tarafında: HTTPS, secrets management, Redis şifresi, Actuator/Swagger kapatma. Bunlar frontend tamamlanmadan önce bir checklist olarak prod'a geçişte yapılması gereken şeyler.

---

## 2. Altyapı Eksikleri — Öncelik Sırası

| Öncelik | Madde |
|---------|-------|
| 🔴 Kritik | PostgreSQL/Redis portları kapat |
| 🔴 Kritik | Nginx + HTTPS ekle |
| 🔴 Kritik | `spring.profiles.active=test` kaldır |
| 🟠 Yüksek | Redis şifre + `volatile-lru` |
| 🟠 Yüksek | Actuator kısıtla |
| 🟡 Orta | Image versiyonları sabitle |
| 🟡 Orta | Watchtower düzelt veya kaldır |

### Detaylar

**1. PostgreSQL ve Redis portları dışarıya açık** — `docker-compose.yaml:41` ve `67`

`postgres` ve `redis` servisleri zaten `ecom-net` ağında olduğundan `app` container'ı onlara container adıyla ulaşıyor. Host port mapping gereksiz. Sunucunun firewall'ı yanlış yapılandırılmışsa bu portlara internetten erişilebilir. Düzeltme: her iki servisten `ports:` bloğunu tamamen kaldır.

**2. HTTPS / Nginx yok**

`app` container'ı doğrudan `${HOST_PORT}:5353` ile dışarıya açılıyor. Önünde SSL sonlandıran reverse proxy yok. Cookie `Secure=false` da bu yüzden — HTTP üzerinden çalışıyor. Prod'da Docker Compose'a `nginx` servisi eklenmeli, Let's Encrypt ile SSL sertifikası alınmalı (Certbot veya Traefik), `app` portunun dışarıya kapatılıp sadece Nginx'in 80/443 açık olması sağlanmalı.

**3. `spring.profiles.active=test` base properties'te** — `application.properties:21`

Her ortamda `test` profili aktif olduğundan prod container başlarken `CreateUserBean` çalışıyor. Bu satır `application.properties`'den kaldırılmalı, profil ortam değişkeniyle verilmeli: `SPRING_PROFILES_ACTIVE=prod`.

**4. Redis şifresiz ve `allkeys-lru`** — `docker-compose.yaml:61`

Şifre yok. `allkeys-lru` ile bellek dolunca session key'leri de silinebilir. Düzeltme: `--requirepass ${REDIS_PASSWORD}` ve `--maxmemory-policy volatile-lru` eklenmeli.

**5. Actuator gereğinden fazla açık** — `application.properties:14-15`

`mappings` tüm endpoint haritasını, `metrics` JVM iç metriklerini veriyor. `show-details=always` DB bağlantı durumu dahil her şeyi açık ediyor. Düzeltme: sadece `health` endpoint'i açık bırakılmalı, `show-details=never` yapılmalı.

**6. Image versiyonları sabitlenmemiş** — `docker-compose.yaml:29` ve `58`

`postgres:latest` ve `redis:latest` her `docker pull`'da farklı sürüm çekebilir. Prod'da breaking change riski var. Düzeltme: `postgres:17`, `redis:7.4` gibi sabit versiyonlar kullanılmalı.

**7. Watchtower label var ama servis yok** — `docker-compose.yaml:26`

`com.centurylinklabs.watchtower.enable=true` label'ı eklenmiş ama `docker-compose.yaml`'da Watchtower servisi tanımlı değil. Ya servis eklenmeli ya da label kaldırılmalı.

# 🛒 E-Commerce Backend API

> **Spring Boot 4.0** tabanlı, production-ready e-ticaret backend uygulaması.

---

## 📑 İçindekiler

- [Genel Bakış](#-genel-bakış)
- [Teknoloji Stack](#-teknoloji-stack)
- [Proje Yapısı](#-proje-yapısı)
- [Ön Gereksinimler](#-ön-gereksinimler)
- [Kurulum ve Çalıştırma](#-kurulum-ve-çalıştırma)
- [Ortam Değişkenleri](#-ortam-değişkenleri-env)
- [Veritabanı Şeması](#-veritabanı-şeması)
- [API Endpoint'leri](#-api-endpointleri)
- [Kimlik Doğrulama Akışı](#-kimlik-doğrulama-akışı)
- [Ödeme Sistemi](#-ödeme-sistemi)
- [Güvenlik Mimarisi](#-güvenlik-mimarisi)
- [Mimari Kararlar](#-mimari-kararlar)

---

## 🔍 Genel Bakış

Tam kapsamlı bir e-ticaret platformunun backend API'si. Temel özellikler:

- **OTP tabanlı passwordless kimlik doğrulama** (e-posta ile)
- **JWT (Access + Refresh Token)** ile stateless session yönetimi
- **RBAC** – Rol ve Permission tabanlı granüler yetkilendirme
- **Ürün kataloğu** – Kategoriler, ürünler, varyantlar (beden/renk), görseller, vitrin/öne çıkan ürünler
- **Sepet ve sipariş akışı** – Checkout preview, sipariş oluşturma, iptal, iade talebi
- **Otomatik sipariş süresi** – PENDING siparişler 60 saniyede bir kontrol edilir; süresi dolan siparişler iptal edilir ve stok geri yüklenir
- **Stok güvenliği** – `@Version` optimistik kilitleme ile eş zamanlı stok tüketiminde tutarsızlık engellenir
- **Ödeme entegrasyonu** – Iyzico Checkout Form + Stripe (Strategy Pattern)
- **İade akışı** – Kullanıcı talebi → Admin onay/red → Iyzico otomatik iade
- **Adres yönetimi** – Ülke/Şehir/İlçe hiyerarşisi ile CRUD
- **Admin paneli** – Ürün, sipariş, kullanıcı, ödeme, iade, rol, site konfigürasyonu
- **Analytics dashboard** – Sipariş, ödeme, ürün ve kullanıcı analizleri
- **Audit log** – Admin işlemlerinin izlenmesi (`admin_audit_logs`)
- **Güvenlik olay logu** – Giriş/çıkış/OTP olayları DB'ye kaydedilir (`security_events`)
- **Ödeme olay logu** – Webhook ham verisi, durum değişimleri, iade, capture, void (`payment_events`)
- **Kullanıcı aktivite logu** – Her HTTP isteği Redis kuyruğu üzerinden asenkron olarak `user_activity_events` tablosuna kaydedilir
- **Domain bazlı log dosyaları** – `auth.log`, `payment.log`, `order.log`, `admin.log`, `error.log`
- **Site konfigürasyonu** – Dinamik ayarlar ve asset slot yönetimi (Redis cache)
- **E-posta servisi** – OTP, iade kodu maili (Gmail SMTP, Redis queue)
- **Redis** – Token, OTP, RBAC cache, e-posta kuyruğu, site config cache, ürün/kategori cache
- **Flyway** – Versiyonlu veritabanı migration yönetimi (V21 mevcut)

---

## 🛠 Teknoloji Stack

| Katman              | Teknoloji                                      |
| ------------------- | ---------------------------------------------- |
| **Framework**       | Spring Boot 4.0, Spring Security, Spring MVC   |
| **Dil**             | Java 21                                        |
| **Veritabanı**      | PostgreSQL (CITEXT, JSONB, Custom ENUM'lar)    |
| **Cache / Queue**   | Redis                                          |
| **ORM**             | Spring Data JPA / Hibernate                    |
| **Migration**       | Flyway                                         |
| **Güvenlik**        | JWT (jjwt 0.13.0), Stateless Session           |
| **Mapping**         | MapStruct 1.6.3                                |
| **Validation**      | Jakarta Bean Validation                        |
| **API Docs**        | SpringDoc OpenAPI 3.0.0 (Swagger UI)           |
| **E-posta**         | Spring Mail (Gmail SMTP)                       |
| **Ödeme**           | Iyzico SDK 2.0.141, Stripe SDK 24.16.0         |
| **Build Tool**      | Maven (Maven Wrapper dahil)                    |
| **Container**       | Docker (Multi-stage), Docker Compose           |
| **Monitoring**      | Spring Boot Actuator                           |
| **Boilerplate**     | Lombok                                         |

---

## 📂 Proje Yapısı

```
ecom/
├── src/
│   └── main/
│       ├── java/selahattin/dev/ecom/
│       │   ├── EcomApplication.java              # @EnableScheduling burada
│       │   │
│       │   ├── config/
│       │   │   ├── AppConfig.java
│       │   │   ├── CacheWarmupRunner.java         # Başlangıçta RBAC cache'i ısıtır
│       │   │   ├── MdcLoggingFilter.java          # TraceID / UserID / IP MDC bağlamı
│       │   │   ├── RedisConfig.java
│       │   │   ├── SecurityConfig.java
│       │   │   ├── SwaggerConfig.java
│       │   │   ├── WebConfig.java                 # RequestLoggingInterceptor kaydı
│       │   │   └── properties/
│       │   │       ├── ClientProperties.java
│       │   │       ├── JwtProperties.java
│       │   │       └── PaymentProperties.java
│       │   │
│       │   ├── controller/
│       │   │   ├── AuthController.java            # Kayıt, giriş, OTP, token yenileme
│       │   │   ├── CartController.java
│       │   │   ├── CategoryController.java
│       │   │   ├── LocationController.java
│       │   │   ├── OrderController.java           # Sipariş akışı + ödeme durumu
│       │   │   ├── PaymentController.java
│       │   │   ├── PaymentWebhookController.java  # Iyzico/Stripe callback
│       │   │   ├── ProductController.java         # Listing, slug, showcase, best-sellers
│       │   │   ├── RefundController.java          # Kullanıcı iade sorgulama
│       │   │   ├── SiteConfigController.java      # Public site ayarları
│       │   │   ├── UserController.java
│       │   │   └── admin/
│       │   │       ├── AdminAuditLogController.java   # Audit log listeleme
│       │   │       ├── AdminCategoriesController.java
│       │   │       ├── AdminOrdersController.java     # Sipariş + iade onay/red + kargo
│       │   │       ├── AdminPaymentsController.java
│       │   │       ├── AdminPermissionController.java
│       │   │       ├── AdminProductsController.java
│       │   │       ├── AdminRefundsController.java
│       │   │       ├── AdminRolesController.java
│       │   │       ├── AdminSiteConfigController.java # Site ayarları + asset upload
│       │   │       ├── AdminUsersController.java
│       │   │       └── AnalyticsController.java       # Analytics dashboard
│       │   │
│       │   ├── dev/
│       │   │   └── CreateUserBean.java            # @Profile({"dev","test"}) — seed verisi
│       │   │
│       │   ├── dto/
│       │   │   ├── infra/                         # Cookie, Token, Email, ActivityLog DTO'ları
│       │   │   ├── request/                       # İstek DTO'ları (Jakarta Validation)
│       │   │   └── response/
│       │   │       ├── admin/                     # AdminOrderResponse, AdminPaymentResponse,
│       │   │       │                              #   AdminRefundResponse, AdminUserResponse,
│       │   │       │                              #   AuditLogResponse
│       │   │       ├── analytics/                 # DashboardAnalyticsResponse,
│       │   │       │                              #   OrderAnalyticsResponse,
│       │   │       │                              #   PaymentAnalyticsResponse,
│       │   │       │                              #   ProductAnalyticsResponse,
│       │   │       │                              #   UserAnalyticsResponse
│       │   │       ├── auth/                      # RoleResponse, PermissionResponse
│       │   │       ├── catalog/                   # CategoryResponse
│       │   │       ├── order/                     # OrderResponse, OrderDetailResponse,
│       │   │       │                              #   OrderSummaryResponse, OrderItemResponse
│       │   │       ├── payment/                   # PaymentResponse, PaymentInitResponse,
│       │   │       │                              #   PaymentCallbackResult, RefundResponse
│       │   │       ├── product/                   # ProductResponse, VariantResponse,
│       │   │       │                              #   ImageResponse
│       │   │       └── site/                      # SiteConfigResponse, AssetResponse,
│       │   │                                      #   AssetSlotResponse
│       │   │
│       │   ├── entity/
│       │   │   ├── audit/                         # AdminAuditLogEntity, SecurityEventEntity,
│       │   │   │                                  #   UserActivityEventEntity
│       │   │   ├── auth/                          # UserEntity, RoleEntity, PermissionEntity
│       │   │   ├── catalog/                       # ProductEntity (isShowcase),
│       │   │   │                                  #   ProductVariantEntity (@Version),
│       │   │   │                                  #   ProductImageEntity, CategoryEntity
│       │   │   ├── location/                      # AddressEntity, CountryEntity,
│       │   │   │                                  #   CityEntity, DistrictEntity
│       │   │   ├── order/                         # CartEntity, CartItemEntity,
│       │   │   │                                  #   OrderEntity, OrderItemEntity
│       │   │   ├── payment/                       # PaymentEntity, RefundEntity,
│       │   │   │                                  #   PaymentEventEntity
│       │   │   └── site/                          # AssetEntity, SiteSettingEntity,
│       │   │                                      #   SiteAssetSlotEntity
│       │   │
│       │   ├── exception/
│       │   │   ├── BusinessException.java
│       │   │   ├── ErrorCode.java
│       │   │   └── GlobalExceptionHandler.java
│       │   │
│       │   ├── repository/
│       │   │   ├── audit/
│       │   │   ├── auth/                          # UserSpecification — JPA Specification
│       │   │   ├── catalog/                       # ProductSpecification
│       │   │   ├── location/
│       │   │   ├── order/                         # OrderSpecification, expiry query, analytics
│       │   │   ├── payment/                       # Analytics + N+1 fix query'leri
│       │   │   └── site/
│       │   │
│       │   ├── response/
│       │   │   └── ApiResponse.java               # {success, message, data, timestamp, errorCode}
│       │   │
│       │   ├── security/
│       │   │   ├── CustomUserDetails.java
│       │   │   ├── CustomUserDetailsService.java
│       │   │   └── jwt/
│       │   │       ├── JwtAuthenticationFilter.java  # Cookie tabanlı token okuma
│       │   │       └── JwtTokenProvider.java
│       │   │
│       │   ├── service/
│       │   │   ├── domain/
│       │   │   │   ├── AuthService.java           # SecurityEventService entegreli
│       │   │   │   ├── CartService.java
│       │   │   │   ├── CategoryService.java
│       │   │   │   ├── OrderExpiryService.java    # @Scheduled 60s — süresi dolan sipariş iptali
│       │   │   │   ├── OrderService.java
│       │   │   │   ├── PaymentService.java        # Webhook işleme, iade, PaymentEventService entegreli
│       │   │   │   ├── PaymentEventService.java   # Ödeme olayları DB kaydı (REQUIRES_NEW)
│       │   │   │   ├── ProductService.java        # Listing'de min variant fiyatı, cache
│       │   │   │   ├── RefundService.java         # Kullanıcı iade sorgulama
│       │   │   │   ├── SecurityEventService.java  # Güvenlik olayları DB kaydı (REQUIRES_NEW)
│       │   │   │   ├── SiteConfigService.java     # Redis cache (TTL: 2 saat)
│       │   │   │   ├── UserAddressService.java
│       │   │   │   ├── UserService.java
│       │   │   │   └── admin/
│       │   │   │       ├── AdminCategoryService.java
│       │   │   │       ├── AdminOrdersService.java    # Audit log entegreli
│       │   │   │       ├── AdminPaymentsService.java  # Audit log + PaymentEvent entegreli
│       │   │   │       ├── AdminPermissionService.java
│       │   │   │       ├── AdminProductsService.java  # Audit log, cache eviction
│       │   │   │       ├── AdminRefundsService.java
│       │   │   │       ├── AdminRoleService.java
│       │   │   │       ├── AdminUsersService.java     # Audit log entegreli
│       │   │   │       ├── AnalyticsService.java      # 5 analitik metot
│       │   │   │       └── AuditLogService.java
│       │   │   ├── infra/
│       │   │   │   ├── CookieService.java
│       │   │   │   ├── FileStorageService.java
│       │   │   │   ├── RedisQueueService.java
│       │   │   │   ├── RequestLogQueueListener.java   # Redis kuyruğundan batch tüketim (50 kayıt / 5s)
│       │   │   │   ├── RoleCacheService.java
│       │   │   │   ├── TokenService.java
│       │   │   │   ├── TokenStoreService.java
│       │   │   │   └── email/
│       │   │   │       ├── EmailQueueListener.java
│       │   │   │       └── EmailService.java
│       │   │   └── integration/payment/
│       │   │       ├── PaymentProviderStrategy.java   # Strategy interface
│       │   │       ├── PaymentStrategyFactory.java
│       │   │       └── impl/
│       │   │           ├── IyzicoPaymentProvider.java # Checkout Form + Refund/Cancel
│       │   │           ├── MockPaymentProvider.java
│       │   │           └── StripePaymentProvider.java
│       │   │
│       │   └── utils/
│       │       ├── SlugUtils.java
│       │       ├── constant/AuthConstant.java
│       │       ├── cookie/CookieFactory.java
│       │       ├── cookie/CookieUtil.java
│       │       └── enums/                         # OrderStatus, PaymentProvider,
│       │                                          #   PaymentStatus, RefundStatus,
│       │                                          #   SecurityEventType, PaymentEventType,
│       │                                          #   CookieConstants
│       │
│       └── resources/
│           ├── application.properties             # Ortak ayarlar (scheduling pool size=2)
│           ├── application-dev.properties         # Lokal geliştirme
│           ├── application-test.properties        # Test ortamı (Tomcat + Lettuce pool)
│           ├── application-prod.properties        # Üretim (env var'dan tüm ayarlar)
│           ├── logback-spring.xml
│           └── db/migration/
│               ├── V1__init_schema.sql
│               ├── V2__insert_countries.sql
│               ├── V3__insert_cities.sql
│               ├── V4__insert_districts.sql
│               ├── V5__insert_roles_permissions.sql
│               ├── V6__add_payment_permissions.sql
│               ├── V7__add_cargo_tracking_to_orders.sql
│               ├── V8__add_return_requested_status.sql
│               ├── V9__add_refund_permissions.sql
│               ├── V10__create_site_config_tables.sql
│               ├── V11__add_site_config_permissions.sql
│               ├── V12__add_analytics_permissions.sql
│               ├── V13__fix_site_settings_value_format.sql
│               ├── V14__add_provider_payment_id_to_payments.sql
│               ├── V15__add_provider_item_transaction_ids_to_payments.sql
│               ├── V16__add_client_ip_to_payments.sql
│               ├── V17__add_security_events_table.sql
│               ├── V18__add_payment_events_table.sql
│               ├── V19__add_user_activity_events_table.sql
│               ├── V20__add_showcase_to_products.sql
│               └── V21__add_version_to_product_variants.sql
│
├── client/
│   ├── test.http                                  # Genel endpoint testleri
│   ├── user.http                                  # Kullanıcı akışı (kayıt→sipariş→iade)
│   └── admin.http                                 # Admin akışı (ürün→sipariş→analytics)
│
├── assets/public/products/                        # Ürün görselleri (local storage)
├── docs/                                          # ⛔ .gitignore — local-only belgeler
│   ├── LOGGING_GUIDE.md
│   └── SECURITY_AUDIT.md
├── logs/                                          # ⛔ .gitignore — runtime log dosyaları
│   ├── ecom-backend.log
│   ├── auth.log / payment.log / order.log
│   ├── admin.log / error.log
│   └── archived/
├── nginx/                                         # Nginx template ve SSL (certbot)
├── Dockerfile
├── docker-compose.yaml
├── docker-entrypoint.sh
├── pom.xml
└── mvnw / mvnw.cmd
```

---

## ✅ Ön Gereksinimler

| Araç                    | Minimum Versiyon |
| ----------------------- | ---------------- |
| **Java JDK**            | 21               |
| **Maven**               | 3.9+ (veya mvnw) |
| **PostgreSQL**          | 14+              |
| **Redis**               | 6+               |
| **Docker** (opsiyonel)  | 24+              |

---

## 🚀 Kurulum ve Çalıştırma

### 1. Lokal Geliştirme (Manuel)

```bash
git clone https://github.com/SelahattinGngr/ecom.git
cd ecom
```

```sql
-- PostgreSQL veritabanını oluştur
CREATE DATABASE ecommerce_db;
```

`application-dev.properties` dosyasını kendi ortamına göre yapılandır (DB bağlantısı, mail, Iyzico API key).

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev    # Linux/macOS
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev  # Windows
```

Uygulama **http://localhost:5353** adresinde başlar.  
Swagger UI: **http://localhost:5353/swagger-ui.html**

> **Dev/Test seed verisi:** `spring.profiles.active=dev` veya `test` ile başlatıldığında `CreateUserBean` otomatik olarak admin/kullanıcı seed verisi oluşturur. İdempotent — tekrar çalıştırmak güvenlidir.

### 2. Docker Compose ile Çalıştırma

```bash
docker compose up --build -d
```

| Container              | Port | Açıklama               |
| ---------------------- | ---- | ---------------------- |
| `{TENANT}_db`          | 5432 | PostgreSQL 18          |
| `{TENANT}_redis`       | 6379 | Redis 8 (maxmem 128MB) |
| `{TENANT}_app`         | 5353 | Spring Boot uygulaması |
| `{TENANT}_nginx`       | 80/443 | Nginx + SSL          |
| `{TENANT}_certbot`     | —    | Let's Encrypt (12s/yenile) |

```bash
docker compose logs -f app   # Logları izle
docker compose down          # Durdur
```

### 3. Sadece Altyapı (DB + Redis)

```bash
docker compose up postgres redis -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 🔐 Ortam Değişkenleri (.env)

```env
# ─── Deployment ───
TENANT_NAME=ecom
HOST_PORT=5353
DOMAIN_NAME=yourdomain.com

# ─── Veritabanı ───
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB_NAME=ecommerce_db
POSTGRES_DB_USER=postgres
POSTGRES_DB_PASSWORD=your_secure_password

# ─── JWT ───
JWT_ACCESS_TOKEN_EXPIRATION_MS=604800000
JWT_REFRESH_TOKEN_EXPIRATION_MS=604800000
JWT_ACCESS_SECRET_KEY=<64-char-hex-secret>
JWT_REFRESH_SECRET_KEY=<64-char-hex-secret>

# ─── Redis ───
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# ─── E-posta (Gmail SMTP) ───
GMAIL_USERNAME=your_email@gmail.com
GMAIL_PASSWORD=your_app_password

# ─── Ödeme ───
PAYMENT_ACTIVE_PROVIDER=IYZICO
PAYMENT_IYZICO_API_KEY=your_sandbox_api_key
PAYMENT_IYZICO_SECRET_KEY=your_sandbox_secret_key
PAYMENT_IYZICO_BASE_URL=https://sandbox-api.iyzipay.com
PAYMENT_STRIPE_API_KEY=sk_test_...
PAYMENT_STRIPE_PUB_KEY=pk_test_...
PAYMENT_STRIPE_WEBHOOK=whsec_...

# ─── Uygulama ───
SERVER_PORT=5353
CLIENT_FRONTEND_URL=http://localhost:3000
CLIENT_BACKEND_URL=http://localhost:5353
CLIENT_EMAIL_VERIFICATION_PATH=/auth/sign-up/verify?id=
CLIENT_CORS_ALLOWED_ORIGINS=http://localhost:3000
```

> ⚠️ `.env` dosyası `.gitignore`'a eklenmiştir. `.env.example` şablonu repoda mevcuttur.

---

## 🗄 Veritabanı Şeması

Şema Flyway ile yönetilir. V1 migration'ı tüm tabloları ve custom ENUM'ları oluşturur.

### Tablo Grupları

```
AUTH & RBAC
  users ──┬── user_roles ── roles ── role_permissions ── permissions
          ├── addresses
          ├── admin_audit_logs      ← admin panel aksiyonları
          ├── security_events       ← giriş/çıkış/OTP güvenlik olayları (V17)
          └── user_activity_events  ← tüm HTTP istek aktiviteleri (V19)

CATALOG
  categories (self-referencing)
  products (is_showcase) ── product_variants (@version optimistik kilit)
                         └── product_images

ORDER & PAYMENT
  carts ── cart_items
  orders ── order_items
         └── payments ──┬── refunds
                        └── payment_events  ← webhook/iade/capture/void izleri (V18)

SITE CONFIGURATION
  assets ── site_asset_slots
  site_settings
```

### Önemli Detaylar

- **Soft Delete:** `users`, `products`, `product_variants`, `categories`, `addresses` — `deleted_at` kolonu
- **UUID PK:** Tüm ana tablolarda `gen_random_uuid()`
- **Custom PostgreSQL ENUM'lar:** `order_status`, `payment_status`, `payment_provider`, `refund_status`
  - Hibernate eşleştirmesi: `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` + `@Enumerated(EnumType.STRING)`
- **CITEXT:** `users.email` — case-insensitive karşılaştırma
- **JSONB:** `orders.shipping_address`, `orders.billing_address`, `site_settings.value_json`, `payments.provider_item_transaction_ids`, `security_events.metadata`, `payment_events.raw_payload`
- **Payments ek kolonlar:** `provider_payment_id` (Iyzico numeric ID, Cancel için), `provider_item_transaction_ids` (per-item ID'ler, Refund için), `client_ip` (fraud prevention)
- **product_variants.version:** `BIGINT NOT NULL DEFAULT 0` — Hibernate `@Version` alanı; eş zamanlı stok güncellemelerinde `OptimisticLockException` fırlatır
- **products.is_showcase:** Vitrin/öne çıkan ürünleri filtreler; `prd:showcase` Redis cache'ini tetikler
- **security_events.user_id:** ON DELETE SET NULL — kullanıcı silinse bile güvenlik geçmişi korunur
- **payment_events.payment_id:** ON DELETE SET NULL — `raw_payload` muhasebe/itiraz kanıtı olarak korunur

---

## 📡 API Endpoint'leri

Tüm yanıtlar standart `ApiResponse<T>` formatındadır:

```json
{
  "success": true,
  "message": "İşlem başarılı",
  "data": { ... },
  "timestamp": "2026-03-24T09:30:00Z",
  "errorCode": null
}
```

> **Auth notu:** JWT token HTTP-Only cookie olarak set edilir. `Authorization: Bearer` header'ı **okunmaz.** Cookie tabanlı istek gönderilmeli.

Base URL: `http://localhost:5353`

### Public API'ler

| HTTP | Endpoint                                          | Açıklama                           |
|------|---------------------------------------------------|------------------------------------|
| POST | `/api/v1/auth/public/signup`                      | Kayıt ol                           |
| POST | `/api/v1/auth/public/signup-verify`               | E-posta doğrula                    |
| POST | `/api/v1/auth/public/resend-verification-email`   | Doğrulama e-postası tekrar gönder  |
| POST | `/api/v1/auth/public/signin`                      | OTP iste                           |
| POST | `/api/v1/auth/public/resend-otp`                  | OTP tekrar gönder                  |
| POST | `/api/v1/auth/public/signin-verify`               | OTP doğrula, token al              |
| POST | `/api/v1/auth/public/refresh-token`               | Access token yenile                |
| GET  | `/api/v1/public/products`                         | Ürün listesi (filtre + sayfalama)  |
| GET  | `/api/v1/public/products/slug/{slug}`             | Ürün detayı                        |
| GET  | `/api/v1/public/products/showcase`                | Vitrin / öne çıkan ürünler (cached)|
| GET  | `/api/v1/public/products/best-sellers`            | En çok satan 20 ürün (cached)      |
| GET  | `/api/v1/public/categories`                       | Kategori ağacı (cached)            |
| GET  | `/api/v1/public/site-config`                      | Site konfigürasyonu (cached)       |
| GET  | `/api/v1/locations/countries`                     | Ülkeler                            |
| GET  | `/api/v1/locations/cities/{countryId}`            | Şehirler                           |
| GET  | `/api/v1/locations/districts/{cityId}`            | İlçeler                            |
| POST | `/api/v1/webhooks/payments/iyzico`                | Iyzico ödeme callback              |
| POST | `/api/v1/webhooks/payments/stripe`                | Stripe webhook                     |

### Authenticated API'ler (JWT Cookie Gerekir)

| HTTP   | Endpoint                               | Açıklama                        |
|--------|----------------------------------------|---------------------------------|
| POST   | `/api/v1/auth/signout`                 | Çıkış yap                      |
| GET    | `/api/v1/users/me`                     | Kullanıcı bilgileri             |
| PATCH  | `/api/v1/users/me`                     | Profil güncelle                 |
| GET    | `/api/v1/users/addresses`              | Adreslerim                      |
| GET    | `/api/v1/users/addresses/{id}`         | Adres detayı                    |
| POST   | `/api/v1/users/addresses`              | Adres ekle                      |
| PATCH  | `/api/v1/users/addresses/{id}`         | Adres güncelle                  |
| DELETE | `/api/v1/users/addresses/{id}`         | Adres sil                       |
| GET    | `/api/v1/users/me/sessions`            | Aktif oturumlar                 |
| DELETE | `/api/v1/users/me/sessions/{deviceId}` | Oturum sonlandır                |
| DELETE | `/api/v1/users/me/sessions`            | Tüm oturumları sonlandır        |
| GET    | `/api/v1/cart`                         | Sepeti getir                    |
| POST   | `/api/v1/cart/items`                   | Sepete ürün ekle                |
| PATCH  | `/api/v1/cart/items/{id}`              | Sepet ürünü güncelle            |
| DELETE | `/api/v1/cart/items/{id}`              | Sepetten ürün çıkar             |
| DELETE | `/api/v1/cart`                         | Sepeti temizle                  |
| POST   | `/api/v1/orders/checkout/preview`      | Checkout özeti                  |
| POST   | `/api/v1/orders/checkout`              | Sipariş oluştur                 |
| GET    | `/api/v1/orders`                       | Siparişlerim                    |
| GET    | `/api/v1/orders/{id}`                  | Sipariş detayı                  |
| GET    | `/api/v1/orders/{id}/payment`          | Sipariş ödeme durumu            |
| POST   | `/api/v1/orders/{id}/cancel`           | Sipariş iptal et                |
| POST   | `/api/v1/orders/{id}/return`           | İade talebi oluştur             |
| POST   | `/api/v1/payments`                     | Ödeme başlat (Iyzico form URL)  |
| GET    | `/api/v1/payments/{id}`                | Ödeme detayı                    |
| GET    | `/api/v1/refunds`                      | İadelerim                       |
| GET    | `/api/v1/refunds/{id}`                 | İade detayı                     |

### Admin API'ler (Permission Gerekir)

| HTTP   | Endpoint                                       | Permission         | Açıklama                      |
|--------|------------------------------------------------|--------------------|-------------------------------|
| GET    | `/api/v1/admin/products`                       | `product:read`     | Ürün listesi                  |
| GET    | `/api/v1/admin/products/slug/{slug}`           | `product:read`     | Ürün detayı (slug)            |
| POST   | `/api/v1/admin/products`                       | `product:create`   | Ürün oluştur (multipart)      |
| PATCH  | `/api/v1/admin/products/{id}`                  | `product:update`   | Ürün güncelle                 |
| DELETE | `/api/v1/admin/products/{id}`                  | `product:delete`   | Ürün sil                      |
| POST   | `/api/v1/admin/products/{id}/variants`         | `product:update`   | Varyant ekle                  |
| DELETE | `/api/v1/admin/products/{id}/variants/{vId}`   | `product:update`   | Varyant sil                   |
| POST   | `/api/v1/admin/products/{id}/images`           | `product:update`   | Görsel ekle                   |
| PATCH  | `/api/v1/admin/products/{id}/images/{iId}`     | `product:update`   | Görsel sırası/thumbnail       |
| DELETE | `/api/v1/admin/products/{id}/images/{iId}`     | `product:update`   | Görsel sil                    |
| GET    | `/api/v1/admin/categories`                     | `category:manage`  | Kategori listesi              |
| POST   | `/api/v1/admin/categories`                     | `category:manage`  | Kategori oluştur              |
| PATCH  | `/api/v1/admin/categories/{slug}`              | `category:manage`  | Kategori güncelle             |
| DELETE | `/api/v1/admin/categories/{slug}`              | `category:manage`  | Kategori sil                  |
| GET    | `/api/v1/admin/orders`                         | `order:read`       | Tüm siparişler (filtreli)     |
| GET    | `/api/v1/admin/orders/{id}`                    | `order:read`       | Sipariş detayı                |
| PATCH  | `/api/v1/admin/orders/{id}/status`             | `order:update`     | Sipariş durumu güncelle       |
| POST   | `/api/v1/admin/orders/{id}/ship`               | `order:update`     | Kargoya ver (kargo + takip)   |
| POST   | `/api/v1/admin/orders/{id}/return/approve`     | `order:update`     | İade onayla → Iyzico iade     |
| POST   | `/api/v1/admin/orders/{id}/return/reject`      | `order:update`     | İade reddet                   |
| GET    | `/api/v1/admin/payments`                       | `payment:read`     | Tüm ödemeler                  |
| GET    | `/api/v1/admin/payments/{id}`                  | `payment:read`     | Ödeme detayı                  |
| POST   | `/api/v1/admin/payments/{id}/capture`          | `payment:manage`   | Ödeme capture                 |
| POST   | `/api/v1/admin/payments/{id}/void`             | `payment:manage`   | Ödeme void                    |
| GET    | `/api/v1/admin/refunds`                        | `refund:read`      | Tüm iadeler                   |
| PATCH  | `/api/v1/admin/refunds/{id}/status`            | `refund:manage`    | İade durumu güncelle          |
| GET    | `/api/v1/admin/users`                          | `user:read`        | Kullanıcılar                  |
| GET    | `/api/v1/admin/users/{id}`                     | `user:read`        | Kullanıcı detayı              |
| PATCH  | `/api/v1/admin/users/{id}/roles`               | `user:manage`      | Kullanıcı rolleri güncelle    |
| DELETE | `/api/v1/admin/users/{id}`                     | `user:manage`      | Kullanıcı sil                 |
| GET    | `/api/v1/admin/roles`                          | `system:manage`    | Rol listesi                   |
| GET    | `/api/v1/admin/roles/{id}`                     | `system:manage`    | Rol detayı                    |
| POST   | `/api/v1/admin/roles`                          | `system:manage`    | Rol oluştur                   |
| PATCH  | `/api/v1/admin/roles/{id}`                     | `system:manage`    | Rol güncelle                  |
| DELETE | `/api/v1/admin/roles/{id}`                     | `system:manage`    | Rol sil                       |
| GET    | `/api/v1/admin/permissions`                    | `system:manage`    | Permission listesi            |
| PATCH  | `/api/v1/admin/site-settings/{key}`            | `site:manage`      | Site ayarı güncelle (upsert)  |
| PATCH  | `/api/v1/admin/site-assets/{slotKey}`          | `site:manage`      | Asset slot güncelle           |
| POST   | `/api/v1/admin/assets/upload`                  | `site:manage`      | Dosya yükle                   |
| GET    | `/api/v1/admin/analytics/dashboard`            | `analytics:read`   | KPI + gelir grafiği           |
| GET    | `/api/v1/admin/analytics/orders`               | `analytics:read`   | Sipariş analitikleri          |
| GET    | `/api/v1/admin/analytics/payments`             | `analytics:read`   | Ödeme analitikleri            |
| GET    | `/api/v1/admin/analytics/products`             | `analytics:read`   | Ürün analitikleri             |
| GET    | `/api/v1/admin/analytics/users`                | `analytics:read`   | Kullanıcı analitikleri        |
| GET    | `/api/v1/admin/logs/audit`                     | `system:manage`    | Audit logları (filtreli)      |

Analytics endpoint'leri `?from=&to=&tz=` query parametresi alır (ISO 8601 OffsetDateTime).

---

## 🔑 Kimlik Doğrulama Akışı

```
KAYIT
  1. POST /auth/public/signup        → { email, firstName, lastName }
  2. POST /auth/public/signup-verify → { verificationId }  → Hesap aktif

GİRİŞ (Passwordless OTP)
  1. POST /auth/public/signin        → { email }           → OTP e-posta gönderilir
  2. POST /auth/public/signin-verify → { email, code }     → accessToken + refreshToken cookie set

TOKEN YENİLEME
  POST /auth/public/refresh-token    → refreshToken cookie → yeni accessToken

ÇIKIŞ
  POST /auth/signout                 → Redis session silinir, cookie'ler temizlenir
```

**Önemli:** JWT filter **sadece cookie** okur. `Authorization: Bearer` header'ı desteklenmez.

---

## 💳 Ödeme Sistemi

Strategy Pattern ile tasarlanmıştır. Yeni provider eklemek için `PaymentProviderStrategy` interface'ini implement etmek yeterlidir.

### Aktif Sağlayıcılar

| Sağlayıcı  | Durum       | Açıklama                              |
|------------|-------------|---------------------------------------|
| **IYZICO** | ✅ Aktif    | Checkout Form entegrasyonu, sandbox   |
| **STRIPE** | 🔧 Entegre  | Stripe SDK, yapılandırılabilir        |
| **MOCK**   | 🧪 Test     | Geliştirme ortamı için               |

### Ödeme Akışı

```
POST /payments
  → PaymentController: X-Forwarded-For / RemoteAddr'dan gerçek IP alınır
  → IyzicoPaymentProvider.initializePayment()
  → client_ip payments tablosuna kaydedilir, Iyzico buyer'a set edilir (fraud prevention)
  → Iyzico'ya CheckoutForm isteği
  → payment_transaction_id = checkout form token (kaydedilir)
  → redirectUrl döner (kullanıcı Iyzico sayfasına yönlenir)

POST /webhooks/payments/iyzico  (form-urlencoded, JWT YOK)
  → CheckoutForm.retrieve(token)
  → provider_payment_id kaydedilir     (Cancel için)
  → provider_item_transaction_ids kaydedilir  (Refund için)
  → PaymentStatus, OrderStatus güncellenir
  → Frontend'e 302 redirect
```

### İade Akışı

```
POST /orders/{id}/return
  → OrderStatus: RETURN_REQUESTED
  → returnCode = "RET-" + orderId (ilk 8 karakter)
  → Kullanıcıya returnCode e-postası gönderilir

POST /admin/orders/{id}/return/approve
  → IyzicoPaymentProvider.refundPayment()
  → Her item için CreateRefundRequest (provider_item_transaction_ids kullanılır)
  → OrderStatus: RETURNED, PaymentStatus: REFUNDED

POST /admin/orders/{id}/return/reject?reason=
  → OrderStatus: PAID'e döner, returnCode temizlenir
  → Kullanıcıya red maili gönderilir
```

### Ödeme Durumları

```
PENDING → REQUIRES_ACTION → SUCCEEDED → REFUNDED
                          → FAILED
                          → CANCELLED
```

---

## 🛡 Güvenlik Mimarisi

```
İstek → CORS Filter → MdcLoggingFilter → JWT Filter → Security Filter Chain → Controller
                                              │
                                              ├─ Cookie'den accessToken oku
                                              ├─ Token geçerli mi? (JwtTokenProvider)
                                              ├─ Redis'te session var mı? (TokenService)
                                              ├─ Rol cache'den permission'ları yükle (RoleCacheService)
                                              └─ @PreAuthorize kontrolleri
```

**Public endpoint whitelist:**

```
/api/v1/public/**
/api/v1/auth/public/**
/api/v1/webhooks/**
/api/v1/locations/**
/assets/public/**
/actuator/health          ← her zaman açık (Docker healthcheck)
/actuator/**              ← dev-tools.enabled=true ise açık
/swagger-ui/**            ← dev-tools.enabled=true ise açık
/v3/api-docs/**           ← dev-tools.enabled=true ise açık
```

**Redis cache anahtarları:**

| Anahtar                         | TTL     | İçerik                          |
|---------------------------------|---------|---------------------------------|
| `auth:signin_otp:{email}`       | 5 dk    | OTP kodu                        |
| `auth:rt:{jti}`                 | refresh | Refresh token session            |
| `auth:user_sessions:{userId}`   | refresh | Kullanıcı session index          |
| `security:roles:{roleName}`     | kalıcı  | Permission listesi               |
| `site:public:config`            | 2 saat  | Site konfigürasyonu             |
| `cat:tree`                      | 24 saat | Kategori ağacı                  |
| `prd:slug:{slug}`               | 10 dk   | Ürün detayı                     |
| `prd:list:{sha256Hash}`         | 5 dk    | Ürün listesi sayfası            |
| `prd:showcase`                  | 24 saat | Vitrin ürünler                  |
| `prd:bestsellers`               | 1 saat  | En çok satan 20 ürün            |

---

## 📋 Loglama

### Domain Bazlı Log Dosyaları

Docker volume `./app/logs:/app/logs` ile host'a bağlıdır. Her log hem ilgili domain dosyasına hem ana `ecom-backend.log`'a düşer.

| Dosya | İçerik | Saklama |
|-------|--------|---------|
| `ecom-backend.log` | Tüm loglar | 30 gün |
| `auth.log` | Giriş, çıkış, OTP, kayıt | 90 gün |
| `payment.log` | Ödeme, webhook, capture, void | 90 gün |
| `order.log` | Checkout, iptal, iade, kargo | 90 gün |
| `admin.log` | Tüm admin panel işlemleri | 90 gün |
| `error.log` | Sadece ERROR seviyesi (tüm domain'ler) | 90 gün |

### Veritabanı Olay Logları

| Tablo | Ne Kaydedilir | Servis |
|-------|---------------|--------|
| `admin_audit_logs` | Admin mutasyonları (durum, iade, rol, ürün silme, capture, void) | `AuditLogService` |
| `security_events` | Giriş başarı/başarısız, OTP hatası, kayıt, çıkış, token yenileme hatası | `SecurityEventService` |
| `payment_events` | Webhook ham payload, ödeme başarı/başarısız, iade, capture, void | `PaymentEventService` |
| `user_activity_events` | Tüm HTTP istekleri (asenkron Redis kuyruğu → batch saveAll 50 kayıt/5s) | `RequestLogQueueListener` |

`SecurityEventService` ve `PaymentEventService` `REQUIRES_NEW` propagation kullanır — log hatası ana iş akışını kesmez.

---

## 🏗 Mimari Kararlar

### Katmanlı Mimari

```
Controller → Service (Domain) → Repository (JPA)
                 ↓
           Service (Infra)    → Redis / Email / File
                 ↓
          Service (Integration) → Iyzico / Stripe
```

### Önemli Desenler

| Desen               | Kullanım                                                          |
|---------------------|-------------------------------------------------------------------|
| **Strategy**        | Ödeme sağlayıcıları (`PaymentProviderStrategy`)                   |
| **Factory**         | `PaymentStrategyFactory` — provider seçimi                        |
| **Specification**   | `UserSpecification`, `ProductSpecification`, `OrderSpecification` |
| **Repository**      | Spring Data JPA, analytics native query'leri                      |
| **DTO**             | Request/Response entity izolasyonu                                |
| **Soft Delete**     | `deleted_at` — veri kaybı önleme                                  |
| **Observer (Queue)**| Redis queue ile asenkron e-posta ve aktivite logu                 |
| **Global Exception**| `@ControllerAdvice` merkezi hata yönetimi                         |
| **Optimistic Lock** | `@Version` on `ProductVariantEntity` — eş zamanlı stok güvenliği |

### Kritik Teknik Notlar

- **PostgreSQL custom ENUM:** JPQL `::OrderStatus` cast üretir (yanlış). Native SQL + `::order_status` cast kullanılmalı.
- **Iyzico Refund:** `CreateRefundRequest.paymentTransactionId` per-item numeric ID bekler; checkout token değil. ID'ler callback sırasında DB'ye kaydedilir.
- **Optimistik kilit:** Stok düşürme `@Version` ile native SQL üzerinden yapılır. `OptimisticLockException` fırlayınca sipariş service'i stok yetersizliği hatası olarak yakalar.
- **Sipariş süresi:** `OrderExpiryService` her 60 saniyede PENDING + süresi dolan siparişleri bulur, stok geri yükler ve `CANCELLED` yapar. `@Scheduled` + `spring.task.scheduling.pool.size=2`.
- **Aktivite logu:** `RequestLoggingInterceptor` her istekte `ActivityLogDto`'yu Redis `request_log_queue`'ya iter — hiçbir zaman istek thread'ini bloke etmez. `RequestLogQueueListener` bu kuyruğu batch'ler halinde tüketir.
- **N+1 Fix:** `AdminPaymentsService` ve `AdminOrdersService` `JOIN FETCH` sorgular kullanır.
- **Ürün listing fiyatı:** `basePrice` değil, aktif variantların minimum fiyatı döner.
- **Vitrin / best-sellers:** Showcase 24 saatlik Redis cache; best-sellers `DELIVERED` siparişlerden hesaplanır, 1 saatlik TTL.

---

## 📄 Lisans

Bu proje özel kullanım amaçlıdır.

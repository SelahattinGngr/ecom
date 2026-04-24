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
- **Ürün kataloğu** – Kategoriler, ürünler, varyantlar (beden/renk), görseller
- **Sepet ve sipariş akışı** – Checkout preview, sipariş oluşturma, iptal, iade talebi
- **Ödeme entegrasyonu** – Iyzico Checkout Form + Stripe (Strategy Pattern)
- **İade akışı** – Kullanıcı talebi → Admin onay/red → Iyzico otomatik iade
- **Adres yönetimi** – Ülke/Şehir/İlçe hiyerarşisi ile CRUD
- **Admin paneli** – Ürün, sipariş, kullanıcı, ödeme, iade, rol, site konfigürasyonu
- **Analytics dashboard** – Sipariş, ödeme, ürün ve kullanıcı analizleri
- **Audit log** – Admin işlemlerinin izlenmesi (`admin_audit_logs`)
- **Güvenlik olay logu** – Giriş/çıkış/OTP olayları DB'ye kaydedilir (`security_events`)
- **Ödeme olay logu** – Webhook ham verisi, durum değişimleri, iade, capture, void (`payment_events`)
- **Domain bazlı log dosyaları** – `auth.log`, `payment.log`, `order.log`, `admin.log`, `error.log`
- **Site konfigürasyonu** – Dinamik ayarlar ve asset slot yönetimi (Redis cache)
- **E-posta servisi** – OTP, iade kodu maili (Gmail SMTP, Redis queue)
- **Redis** – Token, OTP, RBAC cache, e-posta kuyruğu, site config cache
- **Flyway** – Versiyonlu veritabanı migration yönetimi

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
│       │   ├── EcomApplication.java
│       │   │
│       │   ├── config/
│       │   │   ├── AppConfig.java
│       │   │   ├── CacheWarmupRunner.java         # Başlangıçta RBAC cache'i ısıtır
│       │   │   ├── RedisConfig.java
│       │   │   ├── SecurityConfig.java
│       │   │   ├── SwaggerConfig.java
│       │   │   ├── WebConfig.java
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
│       │   │   ├── PaymentWebhookController.java  # Iyzico callback (/webhooks/payments/iyzico)
│       │   │   ├── ProductController.java
│       │   │   ├── RefundController.java          # Kullanıcı iade sorgulama
│       │   │   ├── SiteConfigController.java      # Public site ayarları
│       │   │   ├── UserController.java
│       │   │   └── admin/
│       │   │       ├── AdminAuditLogController.java   # Audit log listeleme
│       │   │       ├── AdminCategoriesController.java
│       │   │       ├── AdminOrdersController.java     # Sipariş + iade onay/red
│       │   │       ├── AdminPaymentsController.java
│       │   │       ├── AdminPermissionController.java
│       │   │       ├── AdminProductsController.java
│       │   │       ├── AdminRefundsController.java
│       │   │       ├── AdminRolesController.java
│       │   │       ├── AdminSiteConfigController.java # Site ayarları + asset upload
│       │   │       ├── AdminUsersController.java
│       │   │       └── AnalyticsController.java       # Analytics dashboard
│       │   │
│       │   ├── dto/
│       │   │   ├── infra/                         # Cookie, Token, Email DTO'ları
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
│       │   │   ├── audit/                         # AdminAuditLogEntity, SecurityEventEntity
│       │   │   ├── auth/                          # UserEntity, RoleEntity, PermissionEntity
│       │   │   ├── catalog/                       # ProductEntity, ProductVariantEntity,
│       │   │   │                                  #   ProductImageEntity, CategoryEntity
│       │   │   ├── location/                      # AddressEntity, CountryEntity,
│       │   │   │                                  #   CityEntity, DistrictEntity
│       │   │   ├── order/                         # CartEntity, CartItemEntity,
│       │   │   │                                  #   OrderEntity, OrderItemEntity
│       │   │   ├── payment/                       # PaymentEntity, RefundEntity, PaymentEventEntity
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
│       │   │   ├── auth/
│       │   │   ├── catalog/
│       │   │   ├── location/
│       │   │   ├── order/                         # Analytics + N+1 fix query'leri içerir
│       │   │   ├── payment/                       # Analytics + N+1 fix query'leri içerir
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
│       │   │   │   ├── OrderService.java
│       │   │   │   ├── PaymentService.java        # Webhook işleme, iade, PaymentEventService entegreli
│       │   │   │   ├── PaymentEventService.java   # Ödeme olayları DB kaydı (REQUIRES_NEW)
│       │   │   │   ├── ProductService.java        # Listing'de min variant fiyatı hesaplar
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
│       │   │   │       ├── AdminProductsService.java  # Audit log entegreli
│       │   │   │       ├── AdminRefundsService.java
│       │   │   │       ├── AdminRoleService.java
│       │   │   │       ├── AdminUsersService.java     # Audit log entegreli
│       │   │   │       ├── AnalyticsService.java      # 5 analitik metot
│       │   │   │       └── AuditLogService.java
│       │   │   ├── infra/
│       │   │   │   ├── CookieService.java
│       │   │   │   ├── FileStorageService.java
│       │   │   │   ├── RedisQueueService.java
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
│       │   ├── utils/
│       │   │   ├── SlugUtils.java
│       │   │   ├── constant/AuthConstant.java
│       │   │   └── enums/                         # OrderStatus, PaymentProvider,
│       │   │                                      #   PaymentStatus, RefundStatus,
│       │   │                                      #   SecurityEventType, PaymentEventType
│       │   │
│       │   └── dev/
│       │       └── CreateUserBean.java            # @Profile({"dev","test"}) — seed verisi
│       │
│       └── resources/
│           ├── application.properties             # Ortak ayarlar
│           ├── application-dev.properties         # Lokal geliştirme
│           ├── application-test.properties        # Test ortamı
│           └── db/migration/
│               ├── V1__init_schema.sql            # Ana şema (tüm tablolar + ENUM'lar)
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
│               └── V18__add_payment_events_table.sql
│
├── client/
│   ├── test.http                                  # Genel endpoint testleri
│   ├── user.http                                  # Kullanıcı akışı (kayıt→sipariş→iade)
│   └── admin.http                                 # Admin akışı (ürün→sipariş→analytics)
│
├── assets/public/products/                        # Ürün görselleri (local storage)
├── docs/                                          # ⛔ .gitignore — local-only belgeler
│   ├── LOGGING_GUIDE.md                           #   Log stratejisi ve rehberi
│   └── SECURITY_AUDIT.md                          #   Güvenlik denetim notları
├── logs/                                          # ⛔ .gitignore — runtime log dosyaları
│   ├── ecom-backend.log                           #   Tüm loglar
│   ├── auth.log / payment.log / order.log         #   Domain bazlı
│   ├── admin.log / error.log                      #   Domain bazlı
│   └── archived/                                  #   Günlük rotasyon
├── Dockerfile
├── docker-compose.yaml
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
# Repoyu klonla
git clone https://github.com/SelahattinGngr/ecom.git
cd ecom
```

```sql
-- PostgreSQL veritabanını oluştur
CREATE DATABASE ecommerce_db;
```

`application-dev.properties` dosyasını kendi ortamına göre yapılandır (DB bağlantısı, mail, Iyzico API key).

```bash
# Maven Wrapper ile çalıştır
./mvnw spring-boot:run          # Linux/macOS
mvnw.cmd spring-boot:run        # Windows
```

Uygulama **http://localhost:5353** adresinde başlar.
Swagger UI: **http://localhost:5353/swagger-ui.html**

> **Dev/Test seed verisi:** `spring.profiles.active=dev` veya `test` ile başlatıldığında `CreateUserBean` otomatik olarak kullanıcı, ürün, sipariş ve ödeme seed verisi oluşturur. Veriler idempotent olduğundan tekrar çalıştırmak güvenlidir.

### 2. Docker Compose ile Çalıştırma

```bash
docker compose up --build -d
```

| Container          | Port | Açıklama              |
| ------------------ | ---- | --------------------- |
| `ecom-postgres`    | 5432 | PostgreSQL            |
| `ecom-redis-cache` | 6379 | Redis cache/queue     |
| `ecom-app`         | 5353 | Spring Boot uygulaması |

```bash
docker compose logs -f app   # Logları izle
docker compose down          # Durdur
```

---

## 🔐 Ortam Değişkenleri (.env)

```env
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

# ─── E-posta (Gmail SMTP) ───
GMAIL_USERNAME=your_email@gmail.com
GMAIL_PASSWORD=your_app_password

# ─── Ödeme ───
IYZICO_API_KEY=your_sandbox_api_key
IYZICO_SECRET_KEY=your_sandbox_secret_key
IYZICO_BASE_URL=https://sandbox-api.iyzipay.com

# ─── Uygulama ───
SERVER_PORT=5353
CLIENT_FRONTEND_URL=http://localhost:3000
CLIENT_BACKEND_URL=http://localhost:5353
```

> ⚠️ `.env` dosyası `.gitignore`'a eklenmiştir.

---

## 🗄 Veritabanı Şeması

Şema Flyway ile yönetilir. V1 migration'ı tüm tabloları ve custom ENUM'ları oluşturur.

### Tablo Grupları

```
AUTH & RBAC
  users ──┬── user_roles ── roles ── role_permissions ── permissions
          ├── addresses
          ├── admin_audit_logs    ← admin panel aksiyonları
          └── security_events     ← giriş/çıkış/OTP güvenlik olayları (V17)

CATALOG
  categories (self-referencing)
  products ── product_variants
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
- **Payments ek kolonlar:** `provider_payment_id` (Iyzico numeric ID, Cancel için), `provider_item_transaction_ids` (per-item IDs, Refund için), `client_ip` (fraud prevention, Iyzico'ya iletilir)
- **security_events:** `user_id` ON DELETE SET NULL — kullanıcı silinse bile güvenlik geçmişi korunur
- **payment_events:** `payment_id` ON DELETE SET NULL — ödeme silinse bile olay kaydı korunur; `raw_payload` sağlayıcının ham webhook verisini saklar (muhasebe/itiraz kanıtı)

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

| HTTP | Endpoint                                          | Açıklama                          |
|------|---------------------------------------------------|-----------------------------------|
| POST | `/api/v1/auth/public/signup`                      | Kayıt ol                         |
| POST | `/api/v1/auth/public/signup-verify`               | E-posta doğrula                  |
| POST | `/api/v1/auth/public/resend-verification-email`   | Doğrulama e-postası tekrar gönder |
| POST | `/api/v1/auth/public/signin`                      | OTP iste                         |
| POST | `/api/v1/auth/public/resend-otp`                  | OTP tekrar gönder                |
| POST | `/api/v1/auth/public/signin-verify`               | OTP doğrula, token al            |
| POST | `/api/v1/auth/public/refresh-token`               | Access token yenile              |
| GET  | `/api/v1/public/products`                         | Ürün listesi (filtre + sayfalama) |
| GET  | `/api/v1/public/products/slug/{slug}`             | Ürün detayı                      |
| GET  | `/api/v1/public/categories`                       | Kategori ağacı                   |
| GET  | `/api/v1/public/site-config`                      | Site konfigürasyonu (cached)     |
| GET  | `/api/v1/locations/countries`                     | Ülkeler                          |
| GET  | `/api/v1/locations/cities/{countryId}`            | Şehirler                         |
| GET  | `/api/v1/locations/districts/{cityId}`            | İlçeler                          |
| POST | `/api/v1/webhooks/payments/iyzico`                | Iyzico ödeme callback            |

### Authenticated API'ler (JWT Cookie Gerekir)

| HTTP   | Endpoint                               | Açıklama                        |
|--------|----------------------------------------|---------------------------------|
| POST   | `/api/v1/auth/signout`                 | Çıkış yap                      |
| GET    | `/api/v1/users/me`                     | Kullanıcı bilgileri             |
| PATCH  | `/api/v1/users/me`                     | Profil güncelle                 |
| GET    | `/api/v1/users/addresses`              | Adreslerim                      |
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
| POST   | `/api/v1/orders/checkout`             | Sipariş oluştur                 |
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
| POST   | `/api/v1/admin/products`                       | `product:create`   | Ürün oluştur (multipart)      |
| PATCH  | `/api/v1/admin/products/{id}`                  | `product:update`   | Ürün güncelle                 |
| DELETE | `/api/v1/admin/products/{id}`                  | `product:delete`   | Ürün sil                      |
| POST   | `/api/v1/admin/products/{id}/variants`         | `product:update`   | Varyant ekle                  |
| DELETE | `/api/v1/admin/products/{id}/variants/{vId}`   | `product:update`   | Varyant sil                   |
| POST   | `/api/v1/admin/products/{id}/images`           | `product:update`   | Görsel ekle                   |
| DELETE | `/api/v1/admin/products/{id}/images/{iId}`     | `product:update`   | Görsel sil                    |
| GET    | `/api/v1/admin/categories`                     | `category:manage`  | Kategori listesi              |
| POST   | `/api/v1/admin/categories`                     | `category:manage`  | Kategori oluştur              |
| PATCH  | `/api/v1/admin/categories/{id}`                | `category:manage`  | Kategori güncelle             |
| DELETE | `/api/v1/admin/categories/{id}`                | `category:manage`  | Kategori sil                  |
| GET    | `/api/v1/admin/orders`                         | `order:read`       | Tüm siparişler                |
| GET    | `/api/v1/admin/orders/{id}`                    | `order:read`       | Sipariş detayı                |
| PATCH  | `/api/v1/admin/orders/{id}/status`             | `order:update`     | Sipariş durumu güncelle       |
| POST   | `/api/v1/admin/orders/{id}/return/approve`     | `order:update`     | İade onayla → Iyzico iade     |
| POST   | `/api/v1/admin/orders/{id}/return/reject`      | `order:update`     | İade reddet                   |
| GET    | `/api/v1/admin/payments`                       | `payment:read`     | Tüm ödemeler                  |
| GET    | `/api/v1/admin/payments/{id}`                  | `payment:read`     | Ödeme detayı                  |
| GET    | `/api/v1/admin/refunds`                        | `refund:read`      | Tüm iadeler                   |
| PATCH  | `/api/v1/admin/refunds/{id}/status`            | `refund:manage`    | İade durumu güncelle          |
| GET    | `/api/v1/admin/users`                          | `user:read`        | Kullanıcılar                  |
| GET    | `/api/v1/admin/users/{id}`                     | `user:read`        | Kullanıcı detayı              |
| PATCH  | `/api/v1/admin/users/{id}/roles`               | `user:manage`      | Kullanıcı rolleri güncelle    |
| DELETE | `/api/v1/admin/users/{id}`                     | `user:manage`      | Kullanıcı sil                 |
| GET    | `/api/v1/admin/roles`                          | `system:manage`    | Rol listesi                   |
| POST   | `/api/v1/admin/roles`                          | `system:manage`    | Rol oluştur                   |
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
  1. POST /auth/public/signup       → { email, firstName, lastName }
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

POST /webhooks/payments/iyzico  (form-urlencoded, Iyzico callback)
  → CheckoutForm.retrieve(token)
  → provider_payment_id kaydedilir     (Cancel için)
  → provider_item_transaction_ids kaydedilir  (Refund için)
  → PaymentStatus, OrderStatus güncellenir
  → Frontend'e 302 redirect
```

### İade Akışı

```
POST /orders/{id}/return          → OrderStatus: RETURN_REQUESTED, returnCode üretilir
POST /admin/orders/{id}/return/approve
  → IyzicoPaymentProvider.refundPayment()
  → Her item için CreateRefundRequest (provider_item_transaction_ids kullanılır)
  → OrderStatus: RETURNED, PaymentStatus: REFUNDED

POST /admin/orders/{id}/return/reject
  → OrderStatus: PAID'e döner, returnCode temizlenir, kullanıcıya red maili
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
İstek → CORS Filter → JWT Filter → Security Filter Chain → Controller
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
/actuator/**          ← health,info,metrics,mappings — production'da kısıtlayın (bkz. docs/SECURITY_AUDIT.md N-05)
/swagger-ui/**
/v3/api-docs/**
```

**Redis cache anahtarları:**

| Anahtar                      | TTL       | İçerik                    |
|------------------------------|-----------|---------------------------|
| `auth:signin_otp:{email}`    | 5 dk      | OTP kodu                  |
| `auth:rt:{jti}`              | refresh   | Refresh token session      |
| `auth:user_sessions:{userId}`| refresh   | Kullanıcı session index   |
| `security:roles:{roleName}`  | kalıcı    | Permission listesi        |
| `site:public:config`         | 2 saat    | Site konfigürasyonu       |
| `cat:tree`                   | 2 saat    | Kategori ağacı            |
| `prd:slug:{slug}`            | 10 dk     | Ürün detayı               |

---

## 📋 Loglama

### Domain Bazlı Log Dosyaları

Docker volume `./logs:/app/logs` ile host'a bağlıdır. Her log hem ilgili domain dosyasına hem ana `ecom-backend.log`'a düşer.

| Dosya | İçerik | Saklama |
|-------|--------|---------|
| `ecom-backend.log` | Tüm loglar | 30 gün |
| `auth.log` | Giriş, çıkış, OTP, kayıt | 90 gün |
| `payment.log` | Ödeme, webhook, capture, void | 90 gün |
| `order.log` | Checkout, iptal, iade, kargo | 90 gün |
| `admin.log` | Tüm admin panel işlemleri | 90 gün |
| `error.log` | Sadece ERROR seviyesi (tüm domain'ler) | 90 gün |

### Veritabanı Olay Logları

Dosya loglarına ek olarak iş açısından kritik olaylar veritabanına kaydedilir:

| Tablo | Ne Kaydedilir | Kimden |
|-------|---------------|--------|
| `admin_audit_logs` | Admin panel mutasyonları (durum değişikliği, iade onay/red, rol güncelleme, ürün silme, capture, void) | `AuditLogService` |
| `security_events` | Giriş başarı/başarısız, OTP hatası, kayıt tamamlama, çıkış, token yenileme hatası | `SecurityEventService` |
| `payment_events` | Webhook ham payload, ödeme başarı/başarısız, iade başlatma/tamamlama, capture, void | `PaymentEventService` |

`SecurityEventService` ve `PaymentEventService` `REQUIRES_NEW` propagation kullanır — log yazma hatası ana iş akışını kesmez, ana transaction rollback olsa bile event kaydı korunur.

Detaylar: `docs/LOGGING_GUIDE.md` (git'e atılmaz)

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

| Desen               | Kullanım                                                 |
|---------------------|----------------------------------------------------------|
| **Strategy**        | Ödeme sağlayıcıları (`PaymentProviderStrategy`)          |
| **Factory**         | `PaymentStrategyFactory` — provider seçimi              |
| **Repository**      | Spring Data JPA, analytics native query'leri            |
| **DTO**             | Request/Response entity izolasyonu                      |
| **Soft Delete**     | `deleted_at` — veri kaybı önleme                        |
| **Observer (Queue)**| Redis queue ile asenkron e-posta                        |
| **Global Exception**| `@ControllerAdvice` merkezi hata yönetimi               |

### Kritik Teknik Notlar

- **PostgreSQL custom ENUM:** JPQL ile string karşılaştırma `::OrderStatus` cast üretir (yanlış). Native SQL + `::order_status` cast kullanılmalı.
- **Iyzico Refund:** `CreateRefundRequest.paymentTransactionId` per-item numeric ID bekler; checkout form token'ı değil. Token'lar ödeme sonrası expire olur — ID'ler callback sırasında DB'ye kaydedilir.
- **N+1 Fix:** `AdminPaymentsService` ve `AdminOrdersService` `JOIN FETCH` sorgular kullanır.
- **Ürün listing fiyatı:** `basePrice` (entity'de statik) değil, aktif variantların minimum fiyatı döner.

---

## 📄 Lisans

Bu proje özel kullanım amaçlıdır.


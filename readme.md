# 🛒 E-Commerce Backend API

> **Spring Boot 4.0** tabanlı, production-ready e-ticaret backend uygulaması.

---

## 📑 İçindekiler

- [Genel Bakış](#-genel-bakış)
- [Teknoloji Stack](#-teknoloji-stack)
- [Proje Yapısı](#-proje-yapısı)
- [Ön Gereksinimler](#-ön-gereksinimler)
- [Kurulum ve Çalıştırma](#-kurulum-ve-çalıştırma)
  - [1. Lokal Geliştirme (Manuel)](#1-lokal-geliştirme-manuel)
  - [2. Docker Compose ile Çalıştırma](#2-docker-compose-ile-çalıştırma)
- [Ortam Değişkenleri (.env)](#-ortam-değişkenleri-env)
- [Veritabanı Şeması](#-veritabanı-şeması)
- [API Endpoint'leri](#-api-endpointleri)
  - [Public API'ler (Giriş Gerekmez)](#public-apiler-giriş-gerekmez)
  - [Authenticated API'ler (JWT Gerekir)](#authenticated-apiler-jwt-gerekir)
  - [Admin API'ler (Yetki Gerekir)](#admin-apiler-yetki-gerekir)
- [Kimlik Doğrulama Akışı](#-kimlik-doğrulama-akışı)
- [Ödeme Sistemi](#-ödeme-sistemi)
- [Güvenlik Mimarisi](#-güvenlik-mimarisi)
- [Mimari Kararlar](#-mimari-kararlar)

---

## 🔍 Genel Bakış

Bu proje, tam kapsamlı bir e-ticaret platformunun backend API'sini içerir. Temel özellikler:

- **OTP tabanlı passwordless kimlik doğrulama** (e-posta ile)
- **JWT (Access + Refresh Token)** ile stateless session yönetimi
- **RBAC (Role-Based Access Control)** – Roller ve Permission'lar ile granüler yetkilendirme
- **Ürün kataloğu** – Kategoriler, ürünler, varyantlar (beden/renk), görseller
- **Sepet yönetimi** – Ürün ekleme, güncelleme, silme
- **Sipariş akışı** – Checkout preview, sipariş oluşturma, iptal, iade talebi
- **Çoklu ödeme sağlayıcı entegrasyonu** – Stripe, Iyzico, Garanti, Mock (Strategy Pattern)
- **Adres yönetimi** – Ülke/Şehir/İlçe hiyerarşisi ile CRUD
- **Admin paneli API'leri** – Ürün, sipariş, kullanıcı, rol, permission, ödeme yönetimi
- **E-posta servisi** – OTP, doğrulama maili (Gmail SMTP, Redis queue)
- **Site konfigürasyonu** – Dinamik site ayarları ve asset slot yönetimi
- **Flyway ile veritabanı migration'ları**
- **Redis** – Token saklama, OTP cache, rol/permission cache, e-posta kuyruğu
- **Swagger/OpenAPI** – Otomatik API dokümantasyonu
- **Docker** – Multi-stage build, production-ready container

---

## 🛠 Teknoloji Stack

| Katman              | Teknoloji                                      |
| ------------------- | ---------------------------------------------- |
| **Framework**       | Spring Boot 4.0, Spring Security, Spring MVC   |
| **Dil**             | Java 21                                        |
| **Veritabanı**      | PostgreSQL (CITEXT, JSONB, Custom ENUM'lar)     |
| **Cache / Queue**   | Redis                                          |
| **ORM**             | Spring Data JPA / Hibernate                    |
| **Migration**       | Flyway                                         |
| **Güvenlik**        | JWT (jjwt 0.13.0), BCrypt, Stateless Session   |
| **Mapping**         | MapStruct 1.6.3                                |
| **Validation**      | Jakarta Bean Validation                        |
| **API Docs**        | SpringDoc OpenAPI 3.0.0 (Swagger UI)           |
| **E-posta**         | Spring Mail (Gmail SMTP)                       |
| **Ödeme**           | Stripe SDK 24.16.0, Iyzico SDK 2.0.141         |
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
│       │   ├── EcomApplication.java              # Spring Boot ana sınıfı
│       │   │
│       │   ├── config/                           # Konfigürasyon sınıfları
│       │   │   ├── AppConfig.java
│       │   │   ├── CacheWarmupRunner.java         # Uygulama başlarken cache'i ısıtır
│       │   │   ├── RedisConfig.java
│       │   │   ├── SecurityConfig.java            # Spring Security yapılandırması
│       │   │   ├── SwaggerConfig.java
│       │   │   ├── WebConfig.java
│       │   │   └── properties/                    # @ConfigurationProperties sınıfları
│       │   │       ├── ClientProperties.java
│       │   │       ├── JwtProperties.java
│       │   │       └── PaymentProperties.java
│       │   │
│       │   ├── controller/                        # REST Controller'lar (Public)
│       │   │   ├── AuthController.java            # Kayıt, giriş, OTP, token yenileme
│       │   │   ├── CartController.java            # Sepet CRUD
│       │   │   ├── CategoryController.java        # Kategori listeleme
│       │   │   ├── LocationController.java        # Ülke/Şehir/İlçe
│       │   │   ├── OrderController.java           # Sipariş akışı
│       │   │   ├── PaymentController.java         # Ödeme başlatma/detay
│       │   │   ├── ProductController.java         # Ürün listeleme/detay
│       │   │   ├── SiteConfigController.java      # Site ayarları
│       │   │   ├── UserController.java            # Profil, adres, session yönetimi
│       │   │   └── admin/                         # Admin Controller'lar
│       │   │       ├── AdminCategoriesController.java
│       │   │       ├── AdminOrdersController.java
│       │   │       ├── AdminPaymentsController.java
│       │   │       ├── AdminPermissionController.java
│       │   │       ├── AdminProductsController.java
│       │   │       ├── AdminRolesController.java
│       │   │       └── AdminUsersController.java
│       │   │
│       │   ├── dto/                               # Data Transfer Objects
│       │   │   ├── infra/                         # Altyapı DTO'ları (Cookie, Token, Email)
│       │   │   ├── request/                       # İstek DTO'ları (validation ile)
│       │   │   └── response/                      # Yanıt DTO'ları
│       │   │
│       │   ├── entity/                            # JPA Entity'leri
│       │   │   ├── BaseEntity.java                # Ortak alanlar (id, createdAt, updatedAt, deletedAt)
│       │   │   ├── audit/                         # AdminAuditLogEntity
│       │   │   ├── auth/                          # UserEntity, RoleEntity, PermissionEntity
│       │   │   ├── catalog/                       # Product, ProductVariant, ProductImage, Category
│       │   │   ├── location/                      # Address, Country, City, District
│       │   │   ├── order/                         # Cart, CartItem, Order, OrderItem
│       │   │   ├── payment/                       # Payment, Refund, RefundItem
│       │   │   └── site/                          # SiteSettings, SiteAssetSlot, SiteConfigAudit, Asset
│       │   │
│       │   ├── exception/                         # Hata yönetimi
│       │   │   ├── BusinessException.java         # Özel iş hatası sınıfı
│       │   │   ├── ErrorCode.java                 # Hata kodları enum'u
│       │   │   └── GlobalExceptionHandler.java    # @ControllerAdvice global handler
│       │   │
│       │   ├── mapper/                            # MapStruct mapper interface'leri
│       │   │
│       │   ├── repository/                        # Spring Data JPA Repository'leri
│       │   │   ├── audit/
│       │   │   ├── auth/
│       │   │   ├── catalog/
│       │   │   ├── location/
│       │   │   ├── order/
│       │   │   ├── payment/
│       │   │   └── site/
│       │   │
│       │   ├── response/                          # Standart API yanıt wrapper'ı
│       │   │   └── ApiResponse.java               # {success, message, data, timestamp, errorCode}
│       │   │
│       │   ├── security/                          # Güvenlik altyapısı
│       │   │   ├── CustomUserDetails.java
│       │   │   ├── CustomUserDetailsService.java
│       │   │   └── jwt/
│       │   │       ├── JwtAuthenticationFilter.java
│       │   │       └── JwtTokenProvider.java
│       │   │
│       │   ├── service/                           # İş mantığı servisleri
│       │   │   ├── domain/                        # Domain servisleri
│       │   │   │   ├── AuthService.java
│       │   │   │   ├── CartService.java
│       │   │   │   ├── CategoryService.java
│       │   │   │   ├── OrderService.java
│       │   │   │   ├── PaymentService.java
│       │   │   │   ├── ProductService.java
│       │   │   │   ├── SiteConfigService.java
│       │   │   │   ├── UserAddressService.java
│       │   │   │   ├── UserService.java
│       │   │   │   └── admin/                     # Admin işlemleri
│       │   │   │       ├── AdminCategoryService.java
│       │   │   │       ├── AdminOrdersService.java
│       │   │   │       ├── AdminPaymentsService.java
│       │   │   │       ├── AdminPermissionService.java
│       │   │   │       ├── AdminProductsService.java
│       │   │   │       ├── AdminRoleService.java
│       │   │   │       └── AdminUsersService.java
│       │   │   ├── infra/                         # Altyapı servisleri
│       │   │   │   ├── CookieService.java
│       │   │   │   ├── FileStorageService.java
│       │   │   │   ├── RedisQueueService.java
│       │   │   │   ├── RoleCacheService.java
│       │   │   │   ├── TokenService.java
│       │   │   │   ├── TokenStoreService.java
│       │   │   │   └── email/
│       │   │   │       ├── EmailQueueListener.java
│       │   │   │       └── EmailService.java
│       │   │   └── integration/                   # Dış entegrasyonlar
│       │   │       └── payment/
│       │   │           ├── PaymentProviderStrategy.java    # Strategy interface
│       │   │           ├── PaymentStrategyFactory.java     # Factory
│       │   │           └── impl/
│       │   │               ├── GarantiPaymentProvider.java
│       │   │               ├── IyzicoPaymentProvider.java
│       │   │               ├── MockPaymentProvider.java
│       │   │               ├── PayTRPaymentProvider.java
│       │   │               └── StripePaymentProvider.java
│       │   │
│       │   ├── utils/                             # Yardımcı sınıflar
│       │   │   ├── SlugUtils.java
│       │   │   ├── constant/AuthConstant.java
│       │   │   ├── cookie/
│       │   │   └── enums/                         # OrderStatus, PaymentProvider, PaymentStatus, vb.
│       │   │
│       │   └── dev/                               # Geliştirme yardımcıları
│       │       ├── CreateUserBean.java
│       │       └── CyberPsychosisBean.java
│       │
│       └── resources/
│           ├── application.properties             # Ortak ayarlar
│           ├── application-dev.properties         # Lokal geliştirme ayarları
│           ├── application-test.properties        # Test ortamı ayarları
│           └── db/migration/                      # Flyway SQL migration'ları
│               ├── V1__init_schema.sql            # Ana şema (tüm tablolar)
│               ├── V2__insert_countries.sql       # Ülke seed verileri
│               ├── V3__insert_cities.sql          # Şehir seed verileri
│               ├── V4__insert_districts.sql       # İlçe seed verileri
│               ├── V5__insert_roles_permissions.sql # Rol/Permission seed verileri
│               └── V6__add_payment_permissions.sql  # Ödeme yetkileri
│
├── client/                                        # HTTP istek dosyaları (test amaçlı)
│   ├── .http
│   └── test.http
│
├── assets/public/products/                        # Ürün görselleri (local storage)
│
├── Dockerfile                                     # Multi-stage Docker build
├── docker-compose.yaml                            # PostgreSQL + Redis + App
├── pom.xml                                        # Maven bağımlılıkları
├── .env                                           # Ortam değişkenleri (Git'te ignore)
├── mvnw / mvnw.cmd                                # Maven Wrapper
└── .gitignore
```

---

## ✅ Ön Gereksinimler

| Araç               | Minimum Versiyon |
| ------------------- | ---------------- |
| **Java JDK**        | 21               |
| **Maven**           | 3.9+ (veya mvnw) |
| **PostgreSQL**      | 14+              |
| **Redis**           | 6+               |
| **Docker** (opsiyonel) | 24+           |
| **Docker Compose** (opsiyonel) | v2+    |

---

## 🚀 Kurulum ve Çalıştırma

### 1. Lokal Geliştirme (Manuel)

**Adım 1: Repoyu klonla**

```bash
git clone https://github.com/SelahattinGngr/ecom.git
cd ecom
```

**Adım 2: PostgreSQL veritabanını oluştur**

```sql
CREATE DATABASE ecommerce_db;
```

PostgreSQL `pgcrypto` ve `citext` extension'ları gereklidir (Flyway migration otomatik oluşturur).

**Adım 3: Redis'i başlat**

```bash
# Lokal Redis sunucusu çalışıyor olmalı
redis-server
```

**Adım 4: `.env` dosyasını yapılandır**

Proje kök dizininde `.env` dosyası oluştur (örnek aşağıda).

**Adım 5: Uygulamayı çalıştır**

```bash
# Maven Wrapper ile (Java 21 kurulu olmalı)
./mvnw spring-boot:run

# Windows:
mvnw.cmd spring-boot:run
```

Uygulama varsayılan olarak **http://localhost:5353** adresinde başlar.

**Adım 6: Swagger UI'a eriş**

```
http://localhost:5353/swagger-ui.html
```

---

### 2. Docker Compose ile Çalıştırma

**Adım 1: `.env` dosyasını yapılandır** (bkz. [Ortam Değişkenleri](#-ortam-değişkenleri-env))

**Adım 2: Docker Compose ile ayağa kaldır**

```bash
docker compose up --build -d
```

Bu komut şu container'ları başlatır:

| Container        | Port   | Açıklama                   |
| ---------------- | ------ | -------------------------- |
| `ecom-postgres`  | 5432   | PostgreSQL veritabanı       |
| `ecom-redis-cache` | 6379 | Redis cache/queue           |
| `ecom-app`       | 5353   | Spring Boot uygulaması      |

**Adım 3: Container loglarını izle**

```bash
docker compose logs -f app
```

**Adım 4: Durdur**

```bash
docker compose down
```

> **Not:** Verileri kalıcı tutmak için Docker volume'ları (`postgres-data`, `redis-data`, `uploads`) kullanılır.

---

## 🔐 Ortam Değişkenleri (.env)

Proje kök dizininde bir `.env` dosyası oluşturun:

```env
# ─── Veritabanı ───
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB_NAME=ecommerce_db
POSTGRES_DB_USER=selahattin
POSTGRES_DB_PASSWORD=your_secure_password

# ─── JWT ───
JWT_ACCESS_TOKEN_EXPIRATION_MS=604800000       # 7 gün
JWT_REFRESH_TOKEN_EXPIRATION_MS=604800000      # 7 gün
JWT_ACCESS_SECRET_KEY=<64-char-hex-secret>
JWT_REFRESH_SECRET_KEY=<64-char-hex-secret>

# ─── Spring Security ───
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=your_admin_password

# ─── Redis ───
REDIS_HOST=redis
REDIS_PORT=6379

# ─── E-posta (Gmail SMTP) ───
GMAIL_USERNAME=your_email@gmail.com
GMAIL_PASSWORD=your_app_password              # Gmail App Password kullanın

# ─── Uygulama ───
SERVER_PORT=5353

# ─── Frontend Client ───
CLIENT_FRONTEND_URL=http://localhost:3000
CLIENT_EMAIL_VERIFICATION_PATH=/auth/sign-up/verify?id=
CLIENT_CORS_ALLOWED_ORIGINS=http://localhost:3000
```

> ⚠️ **`.env` dosyası `.gitignore`'a eklenmiştir.** Hassas bilgilerinizi Git'e commit etmeyin.

---

## 🗄 Veritabanı Şeması

Veritabanı şeması Flyway migration'ları ile yönetilir (`src/main/resources/db/migration/`).

### Entity-Relationship Diyagramı (Tablolar)

```
┌───────────────────────────────────────────────────────────────┐
│                        AUTH & RBAC                            │
├───────────────────────────────────────────────────────────────┤
│  users ──┬── user_roles ── roles ── role_permissions ── permissions
│          ├── addresses                                        │
│          ├── carts ── cart_items                               │
│          ├── orders ── order_items                             │
│          └── admin_audit_logs                                 │
├───────────────────────────────────────────────────────────────┤
│                       CATALOG                                 │
├───────────────────────────────────────────────────────────────┤
│  categories (self-referencing parent_id)                      │
│  products ── product_variants ── product_variant_images       │
│           └── product_images                                  │
├───────────────────────────────────────────────────────────────┤
│                      LOCATION                                 │
├───────────────────────────────────────────────────────────────┤
│  countries ── cities ── districts                             │
├───────────────────────────────────────────────────────────────┤
│                    ORDER & PAYMENT                            │
├───────────────────────────────────────────────────────────────┤
│  orders ── order_items                                        │
│         └── payments ── refunds ── refund_items               │
├───────────────────────────────────────────────────────────────┤
│                  SITE CONFIGURATION                           │
├───────────────────────────────────────────────────────────────┤
│  assets ── site_asset_slots                                   │
│  site_settings                                                │
│  site_config_audit                                            │
└───────────────────────────────────────────────────────────────┘
```

### Önemli Şema Detayları

- **Soft Delete:** `users`, `addresses`, `products`, `product_variants`, `product_images`, `categories` tablolarında `deleted_at` kolonu kullanılır.
- **UUID Primary Key:** Tüm ana tablolarda `gen_random_uuid()` ile otomatik UUID.
- **Custom PostgreSQL ENUM'ları:** `order_status`, `payment_status`, `payment_provider`, `refund_status`.
- **CITEXT:** `users.email` alanı case-insensitive karşılaştırma sağlar.
- **JSONB:** `orders.shipping_address`, `orders.billing_address`, `site_settings.value_json` alanları.
- **Composite Unique Constraints:** Varyant beden/renk kombinasyonu, sepet-ürün benzersizliği vb.
- **Seed Data:** Ülkeler, şehirler, ilçeler, roller ve permission'lar migration'larla otomatik eklenir.

---

## 📡 API Endpoint'leri

Tüm yanıtlar standart `ApiResponse<T>` formatındadır:

```json
{
  "success": true,
  "message": "İşlem başarılı",
  "data": { ... },
  "timestamp": "2026-02-19T18:43:05Z",
  "errorCode": null
}
```

Base URL: `http://localhost:5353`

### Public API'ler (Giriş Gerekmez)

| HTTP  | Endpoint                                  | Açıklama                          |
|-------|-------------------------------------------|-----------------------------------|
| POST  | `/api/v1/auth/public/signup`              | Kayıt ol                         |
| POST  | `/api/v1/auth/public/signup-verify`       | E-posta doğrulama                 |
| POST  | `/api/v1/auth/public/resend-verification-email` | Doğrulama e-postası tekrar gönder |
| POST  | `/api/v1/auth/public/signin`              | OTP iste (giriş adım 1)          |
| POST  | `/api/v1/auth/public/resend-otp`          | OTP tekrar gönder                 |
| POST  | `/api/v1/auth/public/signin-verify`       | OTP doğrula, token al (giriş adım 2) |
| POST  | `/api/v1/auth/public/refresh-token`       | Access token yenile (cookie ile)  |
| GET   | `/api/v1/public/products`                 | Ürün listesi (filtreleme + sayfalama) |
| GET   | `/api/v1/public/products/slug/{slug}`     | Ürün detayı (slug ile)           |
| GET   | `/api/v1/public/categories`               | Tüm kategoriler                   |
| GET   | `/api/v1/public/categories/{id}`          | Kategori detayı                   |
| GET   | `/api/v1/public/site-config`              | Site konfigürasyonu               |
| GET   | `/api/v1/locations/countries`             | Ülke listesi                      |
| GET   | `/api/v1/locations/cities/{countryId}`    | Ülkeye ait şehirler               |
| GET   | `/api/v1/locations/districts/{cityId}`    | Şehre ait ilçeler                 |

### Authenticated API'ler (JWT Gerekir)

| HTTP   | Endpoint                                 | Açıklama                         |
|--------|------------------------------------------|----------------------------------|
| POST   | `/api/v1/auth/signout`                   | Çıkış yap                       |
| GET    | `/api/v1/users/me`                       | Mevcut kullanıcı bilgileri       |
| PATCH  | `/api/v1/users/me`                       | Profil güncelle                  |
| GET    | `/api/v1/users/addresses`                | Adres listesi                    |
| GET    | `/api/v1/users/addresses/{id}`           | Adres detayı                     |
| POST   | `/api/v1/users/addresses`                | Adres ekle                       |
| PATCH  | `/api/v1/users/addresses/{id}`           | Adres güncelle                   |
| DELETE | `/api/v1/users/addresses/{id}`           | Adres sil                        |
| GET    | `/api/v1/users/me/sessions`              | Aktif oturumlar                  |
| DELETE | `/api/v1/users/me/sessions/{deviceId}`   | Tekil oturum sonlandır            |
| DELETE | `/api/v1/users/me/sessions`              | Tüm oturumları sonlandır          |
| GET    | `/api/v1/cart`                           | Sepeti getir                     |
| POST   | `/api/v1/cart/items`                     | Sepete ürün ekle                 |
| PATCH  | `/api/v1/cart/items/{id}`                | Sepet öğesi güncelle             |
| DELETE | `/api/v1/cart/items/{id}`                | Sepetten ürün sil                |
| DELETE | `/api/v1/cart`                           | Sepeti temizle                   |
| POST   | `/api/v1/orders/checkout/preview`        | Checkout özeti oluştur           |
| POST   | `/api/v1/orders/checkout`                | Sipariş oluştur                  |
| GET    | `/api/v1/orders`                         | Siparişlerimi listele            |
| GET    | `/api/v1/orders/{id}`                    | Sipariş detayı                   |
| POST   | `/api/v1/orders/{id}/cancel`             | Sipariş iptal et                 |
| POST   | `/api/v1/orders/{id}/return`             | İade talebi oluştur              |
| POST   | `/api/v1/payments`                       | Ödeme başlat                     |
| GET    | `/api/v1/payments/{id}`                  | Ödeme detayı                     |

### Admin API'ler (Yetki Gerekir)

Tüm admin endpoint'leri `@PreAuthorize` ile korunur. İlgili permission'a sahip role gerekir.

| HTTP   | Endpoint                                          | Permission       | Açıklama                   |
|--------|---------------------------------------------------|------------------|-----------------------------|
| POST   | `/api/v1/admin/products`                          | `product:create` | Ürün oluştur (multipart)    |
| PATCH  | `/api/v1/admin/products/{id}`                     | `product:update` | Ürün güncelle               |
| DELETE | `/api/v1/admin/products/{id}`                     | `product:delete` | Ürün sil (soft delete)      |
| POST   | `/api/v1/admin/products/{id}/variants`            | `product:update` | Varyant ekle                |
| DELETE | `/api/v1/admin/products/{pId}/variants/{vId}`     | `product:update` | Varyant sil                 |
| POST   | `/api/v1/admin/products/{id}/images`              | `product:update` | Görsel ekle                 |
| DELETE | `/api/v1/admin/products/{pId}/images/{iId}`       | `product:update` | Görsel sil                  |
| GET    | `/api/v1/admin/orders`                            | `order:read`     | Tüm siparişleri listele    |
| GET    | `/api/v1/admin/orders/{id}`                       | `order:read`     | Sipariş detayı              |
| PATCH  | `/api/v1/admin/orders/{id}/status`                | `order:update`   | Sipariş durumu güncelle     |
| GET    | `/api/v1/admin/users`                             | `user:read`      | Kullanıcıları listele       |
| GET    | `/api/v1/admin/users/{id}`                        | `user:read`      | Kullanıcı detayı            |
| PATCH  | `/api/v1/admin/users/{id}/roles`                  | `user:manage`    | Kullanıcı rollerini güncelle |
| DELETE | `/api/v1/admin/users/{id}`                        | `user:manage`    | Kullanıcı sil (soft delete) |

> Admin controller'lar ayrıca **Kategoriler** (`AdminCategoriesController`), **Roller** (`AdminRolesController`), **Permission'lar** (`AdminPermissionController`) ve **Ödemeler** (`AdminPaymentsController`) için endpoint'ler içerir.

---

## 🔑 Kimlik Doğrulama Akışı

Proje **passwordless OTP tabanlı** kimlik doğrulama kullanır:

```
┌──────────────────────────────────────────────────────────────────────┐
│                        KAYIT (Sign Up)                               │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. POST /auth/public/signup                                         │
│     └─→ { email, firstName, lastName }                              │
│     └─→ Kullanıcı oluşturulur, doğrulama e-postası gönderilir      │
│                                                                      │
│  2. POST /auth/public/signup-verify                                  │
│     └─→ { verificationId }                                          │
│     └─→ E-posta doğrulanır, hesap aktif olur                        │
│                                                                      │
├──────────────────────────────────────────────────────────────────────┤
│                        GİRİŞ (Sign In - OTP)                        │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. POST /auth/public/signin                                         │
│     └─→ { email }                                                   │
│     └─→ 6 haneli OTP kodu Redis'e kaydedilir, e-posta gönderilir   │
│                                                                      │
│  2. POST /auth/public/signin-verify                                  │
│     └─→ { email, otp }                                              │
│     └─→ OTP doğrulanır, JWT token'lar üretilir                     │
│     └─→ accessToken ve refreshToken HTTP-Only Cookie olarak set     │
│                                                                      │
├──────────────────────────────────────────────────────────────────────┤
│                    TOKEN YENİLEME (Refresh)                          │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  POST /auth/public/refresh-token                                     │
│     └─→ refreshToken cookie'den okunur                              │
│     └─→ Yeni accessToken üretilir                                   │
│                                                                      │
├──────────────────────────────────────────────────────────────────────┤
│                        ÇIKIŞ (Sign Out)                              │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  POST /auth/signout                                                  │
│     └─→ Redis'teki refresh token silinir                            │
│     └─→ Cookie'ler temizlenir                                       │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 💳 Ödeme Sistemi

Ödeme sistemi **Strategy Pattern** ile tasarlanmıştır. Yeni ödeme sağlayıcıları eklemek için sadece `PaymentProviderStrategy` interface'ini implement etmeniz yeterlidir.

### Desteklenen Sağlayıcılar

| Sağlayıcı | Durum     | Açıklama                              |
|-----------|-----------|---------------------------------------|
| **MOCK**  | ✅ Aktif  | Geliştirme/test için sahte ödeme      |
| **Stripe**| 🔧 Entegre | Stripe SDK ile gerçek ödeme           |
| **Iyzico**| 🔧 Entegre | Iyzico sandbox/production            |
| **Garanti**| 🔧 Entegre | Garanti sanal POS                    |
| **PayTR** | 🔧 Entegre | PayTR ödeme sağlayıcısı              |

### Mimari

```
PaymentController
    └─→ PaymentService
            └─→ PaymentStrategyFactory.getProvider(PaymentProvider)
                    ├─→ StripePaymentProvider
                    ├─→ IyzicoPaymentProvider
                    ├─→ GarantiPaymentProvider
                    ├─→ PayTRPaymentProvider
                    └─→ MockPaymentProvider
```

Aktif sağlayıcı `application-dev.properties` dosyasında belirlenir:

```properties
selahattin.dev.payment.active-provider=MOCK
```

### Ödeme Durumları

`PENDING` → `REQUIRES_ACTION` → `SUCCEEDED` / `FAILED` / `CANCELLED` → `REFUNDED`

---

## 🛡 Güvenlik Mimarisi

```
İstek ──→ CORS Filter ──→ JWT Authentication Filter ──→ Security Filter Chain ──→ Controller
                                    │
                                    ├─ Token geçerli mi? (JwtTokenProvider)
                                    ├─ Token Redis'te var mı? (TokenStoreService)
                                    ├─ Kullanıcı aktif mi? (CustomUserDetailsService)
                                    └─ @PreAuthorize kontrolleri (Method Security)
```

### Önemli Güvenlik Detayları

- **Stateless Session:** `SessionCreationPolicy.STATELESS` – Sunucu tarafında session tutulmaz.
- **CSRF Disabled:** SPA (Single Page Application) mimarisinde CSRF koruması devre dışı; CORS aktif.
- **JWT Cookie:** Token'lar HTTP-Only cookie'lerde saklanır (XSS koruması).
- **BCrypt:** Şifre hash'leme (OTP tabanlı akışta kullanılmasa da altyapı hazır).
- **Method Security:** `@PreAuthorize("hasAuthority('...')")` ile granüler yetkilendirme.
- **Rol Caching:** Roller ve permission'lar Redis'te cache'lenir (`RoleCacheService`).
- **Multi-Device Session:** `deviceId` ile cihaz bazlı oturum yönetimi.

### Public Endpoint'ler (Whitelist)

```
/api/v1/public/**          → Herkes erişebilir
/api/v1/auth/public/**     → Herkes erişebilir
/assets/public/**          → Statik dosyalar
/actuator/**               → Health/Info endpoint'leri
/v3/api-docs/**            → OpenAPI spec
/swagger-ui/**             → Swagger UI
```

---

## 🏗 Mimari Kararlar

### Katmanlı Mimari

```
Controller (REST)  →  Service (Domain)  →  Repository (Data Access)
                           ↓
                    Service (Infra)     →  Redis / Email / File Storage
                           ↓
                  Service (Integration) →  Stripe / Iyzico / Garanti / PayTR
```

### Önemli Desenler

| Desen                    | Kullanım Yeri                                                |
|--------------------------|-------------------------------------------------------------|
| **Strategy Pattern**     | Ödeme sağlayıcıları (`PaymentProviderStrategy`)             |
| **Factory Pattern**      | `PaymentStrategyFactory` – Provider seçimi                  |
| **Builder Pattern**      | DTO ve Entity oluşturma (Lombok `@Builder`)                 |
| **Repository Pattern**   | Spring Data JPA Repository'leri                             |
| **DTO Pattern**          | Request/Response objeleri ile entity izolasyonu              |
| **Mapper Pattern**       | MapStruct ile otomatik entity↔DTO dönüşümü                 |
| **Template Method**      | `BaseEntity` – Ortak alanlar (id, timestamps, soft delete)  |
| **Observer (Queue)**     | Redis queue ile asenkron e-posta gönderimi                  |
| **Global Exception**     | `@ControllerAdvice` ile merkezi hata yönetimi               |
| **Soft Delete**          | `deleted_at` ile veri kaybı önleme                           |

### Profil Yapılandırması

| Profil  | Dosya                       | Kullanım                              |
|---------|----------------------------|---------------------------------------|
| `dev`   | `application-dev.properties` | Lokal geliştirme (localhost bağlantıları) |
| `test`  | `application-test.properties`| Test ortamı                             |
| (base)  | `application.properties`    | Ortak ayarlar (JPA, Flyway, Actuator)   |

Aktif profil `application.properties` içinde belirlenir:

```properties
spring.profiles.active=dev
```

### Docker Mimarisi

Dockerfile **multi-stage build** kullanır:

1. **Build Stage:** `maven:3.9.6-eclipse-temurin-21` – Bağımlılıkları indir, uygulamayı derle.
2. **Runtime Stage:** `eclipse-temurin:21-jdk-alpine` – Minimal image, non-root user (`spring`), healthcheck tanımlı.

```
JVM Ayarları: -Xms256m -Xmx512m
Timezone:     Europe/Istanbul
Port:         5353
Healthcheck:  /actuator/health (30s interval)
```

---

## 📄 Lisans

Bu proje özel kullanım amaçlıdır.

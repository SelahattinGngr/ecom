# Production Deployment Guide

Bu döküman, uygulamanın sıfırdan bir VDS/VPS sunucusuna nasıl deploy edileceğini,
SSL sertifikasının nasıl alınacağını, otomatik yenilemenin nasıl çalıştığını ve bakım
prosedürlerini eksiksiz olarak anlatır.

---

## İçindekiler

1. [Mimari Genel Bakış](#1-mimari-genel-bakış)
2. [Sunucu Gereksinimleri](#2-sunucu-gereksinimleri)
3. [Sunucu Hazırlığı — Ubuntu 24.04](#3-sunucu-hazırlığı--ubuntu-2404)
4. [Docker Kurulumu](#4-docker-kurulumu)
5. [Projeyi Sunucuya Aktarma](#5-projeyi-sunucuya-aktarma)
6. [Ortam Değişkenleri (.env)](#6-ortam-değişkenleri-env)
7. [İzin ve Klasör Hazırlığı](#7-i̇zin-ve-klasör-hazırlığı)
8. [İlk SSL Sertifikası](#8-i̇lk-ssl-sertifikası)
9. [Nginx Konfigürasyonu](#9-nginx-konfigürasyonu)
10. [Uygulamayı Başlatma](#10-uygulamayı-başlatma)
11. [Sağlık Kontrolü ve Doğrulama](#11-sağlık-kontrolü-ve-doğrulama)
12. [SSL Otomatik Yenileme](#12-ssl-otomatik-yenileme)
13. [Bakım ve Operasyon](#13-bakım-ve-operasyon)
14. [Log Yönetimi](#14-log-yönetimi)
15. [Yedekleme](#15-yedekleme)
16. [Sorun Giderme](#16-sorun-giderme)
17. [Yeni Müşteri Deployment (Multi-Tenant)](#17-yeni-müşteri-deployment-multi-tenant)

---

## 1. Mimari Genel Bakış

```
İnternet
    │
    ▼
[Nginx :80/:443]  ←── Let's Encrypt SSL (otomatik yenileme)
    │
    │  proxy_pass http://app:5353
    ▼
[Spring Boot App :5353]
    │              │
    ▼              ▼
[PostgreSQL]   [Redis]
   :5432         :6379

Certbot (arka planda, 12 saatte bir renew kontrolü)
```

### Neden bu mimari?

- **Nginx** SSL termination yapar. Uygulama her zaman HTTP üzerinden çalışır, TLS yükünü taşımaz.
- **Certbot** `volatile-lru` Redis ile birlikte çalışır; sertifika yenileme nginx'i durdurmaz (webroot modu).
- **Spring Boot** prod profilinde çalışır: Swagger kapalı, Cookie `Secure: true`, gerçek IP Nginx header'larından okunur.
- **PostgreSQL ve Redis** aynı Docker network'te (`ecom-net`), dışarıya sadece debug için port açık.

---

## 2. Sunucu Gereksinimleri

| Kaynak   | Minimum     | Önerilen     |
|----------|-------------|--------------|
| CPU      | 2 vCPU      | 4 vCPU       |
| RAM      | 2 GB        | 4 GB         |
| Disk     | 20 GB SSD   | 40 GB SSD    |
| İşletim Sistemi | Ubuntu 22.04+ | Ubuntu 24.04 LTS |
| Domain   | A kaydı sunucu IP'sine yönlendirilmiş olmalı | — |

**Gerekli açık portlar:**

| Port | Protokol | Açıklama |
|------|----------|----------|
| 22   | TCP | SSH erişimi |
| 80   | TCP | HTTP (Let's Encrypt challenge + HTTPS yönlendirme) |
| 443  | TCP | HTTPS |

> **Not:** PostgreSQL (5432) ve Redis (6379) portlarını internete açık bırakmayın.
> Sadece kendi IP adresinizden erişim için: `ufw allow from <IP> to any port 5432`

---

## 3. Sunucu Hazırlığı — Ubuntu 24.04

SSH ile bağlanın:

```bash
ssh root@<SUNUCU_IP>
```

Sistemi güncelleyin:

```bash
apt update && apt upgrade -y
```

> Kernel güncellemesi varsa reboot yapın: `reboot`
> Sonra tekrar bağlanın.

---

## 4. Docker Kurulumu

```bash
# Bağımlılıklar
apt-get install -y ca-certificates curl gnupg

# Docker GPG anahtarı
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

# Docker repository ekle
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu \
$(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
| tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker yükle
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

Doğrulama:

```bash
docker --version
docker compose version
```

---

## 5. Projeyi Sunucuya Aktarma

### Yöntem A: SCP (Git push yapılmadıysa)

Local makinede önce `target/` temizle:

```bash
./mvnw clean
```

Sonra transfer et (Windows PowerShell):

```powershell
scp -r C:\Users\kullanici\Desktop\ecom root@<SUNUCU_IP>:/opt/ecom
```

Transfer sonrası `.env` satır sonlarını düzelt (Windows'tan geldiyse CRLF→LF):

```bash
sed -i 's/\r//' /opt/ecom/.env
```

### Yöntem B: Git Clone (Önerilen — CI/CD için)

```bash
git clone https://github.com/<kullanici>/ecom.git /opt/ecom
cd /opt/ecom
```

---

## 6. Ortam Değişkenleri (.env)

```bash
cp /opt/ecom/.env.example /opt/ecom/.env
nano /opt/ecom/.env
```

### Güvenli değer üretme

Her çalıştırmada farklı 48 karakterlik random string üretir:

```bash
openssl rand -base64 48
```

Bunu 3–4 kez çalıştırıp aşağıdaki değerlere atayın.

### Tam .env şablonu

```env
# ── Uygulama ──────────────────────────────────────────────
SERVER_PORT=5353
HOST_PORT=5353
TENANT_NAME=ecom                        # Container isimlerinde prefix olarak kullanılır

# ── Veritabanı ────────────────────────────────────────────
POSTGRES_HOST=postgres                  # Docker service adı (docker-compose içinde)
POSTGRES_PORT=5432
POSTGRES_DB_NAME=ecommerce_db
POSTGRES_DB_USER=dbkullanici
POSTGRES_DB_PASSWORD=<openssl ile üret> # Min 32 karakter, özel karakter içersin

# ── JWT ───────────────────────────────────────────────────
JWT_ACCESS_TOKEN_EXPIRATION_MS=900000   # 15 dakika (access token)
JWT_REFRESH_TOKEN_EXPIRATION_MS=604800000 # 7 gün (refresh token)
JWT_ACCESS_SECRET_KEY=<openssl ile üret>
JWT_REFRESH_SECRET_KEY=<openssl ile üret>

# ── Redis ─────────────────────────────────────────────────
REDIS_HOST=redis                        # Docker service adı
REDIS_PORT=6379
REDIS_PASSWORD=<openssl ile üret>

# ── E-posta (Gmail App Password) ──────────────────────────
# Gmail → Hesap Güvenliği → 2FA açık → Uygulama Şifreleri
GMAIL_USERNAME=ornek@gmail.com
GMAIL_PASSWORD=xxxx xxxx xxxx xxxx     # 16 karakterlik App Password

# ── Client / CORS ─────────────────────────────────────────
CLIENT_FRONTEND_URL=https://domain.com
CLIENT_BACKEND_URL=https://domain.com
CLIENT_EMAIL_VERIFICATION_PATH=/auth/sign-up/verify?id=
CLIENT_CORS_ALLOWED_ORIGINS=https://domain.com

# ── Ödeme Sağlayıcısı ─────────────────────────────────────
# Geçerli değerler: MOCK, STRIPE, IYZICO
PAYMENT_ACTIVE_PROVIDER=MOCK

# Stripe (PAYMENT_ACTIVE_PROVIDER=STRIPE ise doldur)
PAYMENT_STRIPE_API_KEY=sk_live_...
PAYMENT_STRIPE_PUB_KEY=pk_live_...
PAYMENT_STRIPE_WEBHOOK=whsec_...

# Iyzico (PAYMENT_ACTIVE_PROVIDER=IYZICO ise doldur)
PAYMENT_IYZICO_API_KEY=...
PAYMENT_IYZICO_SECRET_KEY=...
PAYMENT_IYZICO_BASE_URL=https://api.iyzipay.com  # Prod URL (sandbox değil!)

# ── Nginx / SSL ───────────────────────────────────────────
DOMAIN_NAME=domain.com                  # Sertifika bu domain için alınır
CERT_EMAIL=admin@domain.com             # Let's Encrypt bildirimleri buraya gider
```

### ÖNEMLİ: Spring profili

`docker-compose.yaml` içindeki `SPRING_PROFILES_ACTIVE` değerini `prod` olarak ayarlayın:

```bash
sed -i 's/SPRING_PROFILES_ACTIVE: test/SPRING_PROFILES_ACTIVE: prod/' /opt/ecom/docker-compose.yaml
```

---

## 7. İzin ve Klasör Hazırlığı

Uygulama `spring` adlı non-root kullanıcıyla çalışır. Host klasörlerinin yazma izni olması gerekir:

```bash
mkdir -p /opt/ecom/app/logs \
         /opt/ecom/app/uploads \
         /opt/ecom/app/db-data \
         /opt/ecom/app/redis-data \
         /opt/ecom/app/certbot/conf \
         /opt/ecom/app/certbot/www

chmod 777 /opt/ecom/app/logs /opt/ecom/app/uploads
```

---

## 8. İlk SSL Sertifikası

### Neden bu adımı önceden yapıyoruz?

Nginx, başlarken SSL sertifika dosyalarının var olmasını bekler. Sertifika yoksa nginx crash eder.
Bu da "tavuk-yumurta" problemi yaratır: nginx başlamadan certbot webroot doğrulaması çalışmaz.

**Çözüm:** İlk sertifikayı nginx olmadan `--standalone` moduyla alıyoruz. Certbot kendi geçici HTTP server'ını port 80'de açıyor.

### Adımlar

**1. Veritabanı ve Redis'i başlat:**

```bash
cd /opt/ecom
docker compose up -d postgres redis
```

**2. Certbot standalone ile sertifika al:**

```bash
docker run --rm \
  -p 80:80 \
  -v /opt/ecom/app/certbot/conf:/etc/letsencrypt \
  certbot/certbot certonly \
  --standalone \
  --email <CERT_EMAIL> \
  --agree-tos \
  --no-eff-email \
  -d <DOMAIN_NAME>
```

Başarılı çıktı:
```
Successfully received certificate.
Certificate is saved at: /etc/letsencrypt/live/<domain>/fullchain.pem
Key is saved at:         /etc/letsencrypt/live/<domain>/privkey.pem
This certificate expires on YYYY-MM-DD.
```

**3. Renewal config'i webroot moduna güncelle:**

Standalone ile alınan sertifikanın renewal config'i de standalone olarak kaydedilir.
Nginx çalışırken renewal için webroot moduna geçmek gerekir:

```bash
cat > /opt/ecom/app/certbot/conf/renewal/<DOMAIN_NAME>.conf << EOF
version = 5.5.0
archive_dir = /etc/letsencrypt/archive/<DOMAIN_NAME>
cert = /etc/letsencrypt/live/<DOMAIN_NAME>/cert.pem
privkey = /etc/letsencrypt/live/<DOMAIN_NAME>/privkey.pem
chain = /etc/letsencrypt/live/<DOMAIN_NAME>/chain.pem
fullchain = /etc/letsencrypt/live/<DOMAIN_NAME>/fullchain.pem

[renewalparams]
account = <account_id>
authenticator = webroot
webroot_path = /var/www/certbot
server = https://acme-v02.api.letsencrypt.org/directory
key_type = ecdsa

[webroot_map]
<DOMAIN_NAME> = /var/www/certbot
EOF
```

> `account` değerini bulmak için:
> ```bash
> ls /opt/ecom/app/certbot/conf/accounts/acme-v02.api.letsencrypt.org/directory/
> ```
> Çıkan klasör adı account ID'sidir.

---

## 9. Nginx Konfigürasyonu

Nginx, `nginx/templates/default.conf.template` dosyasını okur. Docker'ın resmi nginx image'ı bu dosyadaki `${DOMAIN_NAME}` ifadesini başlangıçta `envsubst` ile gerçek domain adına çevirir.

### `nginx/templates/default.conf.template` içeriği

```nginx
server {
    listen 80;
    server_name ${DOMAIN_NAME};

    # Let's Encrypt ACME challenge — renewal için nginx kapatılmaz
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    # Tüm HTTP trafiği HTTPS'e yönlendir
    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    server_name ${DOMAIN_NAME};

    ssl_certificate /etc/letsencrypt/live/${DOMAIN_NAME}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN_NAME}/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    client_max_body_size 10M;

    location / {
        proxy_pass http://app:5353;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 10. Uygulamayı Başlatma

### Tam stack başlatma

```bash
cd /opt/ecom
docker compose up -d --build
```

Bu komut sırasıyla:
1. Maven ile Spring Boot jar'ı build eder (~3-5 dakika ilk seferinde)
2. PostgreSQL ve Redis sağlıklı olana kadar bekler
3. Spring Boot uygulamasını başlatır
4. Nginx'i başlatır
5. Certbot renewal loop'unu başlatır

### Sadece uygulamayı yeniden build etmek

```bash
docker compose up -d --build app
```

### Logları canlı izlemek

```bash
# Tüm servisler
docker compose logs -f

# Sadece uygulama
docker compose logs -f app

# Son 100 satır
docker compose logs --tail=100 app
```

---

## 11. Sağlık Kontrolü ve Doğrulama

```bash
# HTTP yanıt kodu kontrolü
curl -I https://<DOMAIN_NAME>/actuator/health

# JSON yanıt
curl https://<DOMAIN_NAME>/actuator/health
# Beklenen: {"groups":["liveness","readiness"],"status":"UP"}
```

Tüm container'ların durumu:

```bash
docker compose ps
```

Beklenen çıktı:

```
NAME            IMAGE                  STATUS
ecom_app        ecom-app              Up (healthy)
ecom_db         postgres:18           Up (healthy)
ecom_redis      redis:8               Up (healthy)
ecom_nginx      nginx:1.27-alpine     Up
ecom_certbot    certbot/certbot       Up
```

---

## 12. SSL Otomatik Yenileme

### Nasıl çalışır?

```
Certbot container (her 12h)
    │
    ├── certbot renew --quiet
    │       │
    │       ├── Sertifika 30 günden fazla geçerliyse → Hiçbir şey yapma
    │       └── 30 günden az kaldıysa → Webroot doğrulama yap
    │               │
    │               ├── /var/www/certbot/.well-known/acme-challenge/ dosya yaz
    │               ├── Let's Encrypt bu dosyayı HTTP üzerinden kontrol eder
    │               │   (nginx port 80'de bu path'i sunuyor, kapatmaya gerek yok)
    │               └── Yeni sertifika /etc/letsencrypt/live/ altına yazılır
    │
Cron job (her gece 03:00)
    └── nginx -s reload
            └── Nginx yeni sertifika dosyalarını yükler
```

### Nginx reload cron job kurulumu

```bash
(crontab -l 2>/dev/null; echo "0 3 * * * cd /opt/ecom && docker compose exec -T nginx nginx -s reload >> /var/log/nginx-reload.log 2>&1") | crontab -
```

Kontrol:

```bash
crontab -l
```

### Manuel renewal testi

```bash
# Dry-run (gerçek sertifika almaz, sadece test eder)
docker compose exec certbot certbot renew --dry-run
```

Başarılı çıktı:
```
Simulating renewal of an existing certificate for <domain>
Congratulations, all simulated renewals succeeded: ...
```

---

## 13. Bakım ve Operasyon

### Servisleri yeniden başlatma

```bash
# Tüm stack
docker compose restart

# Tek servis
docker compose restart app
docker compose restart nginx
```

### Servisleri durdurma

```bash
# Durdur (veri silinmez)
docker compose down

# Durdur + volume'ları sil (VERİ SİLİNİR!)
docker compose down -v
```

### Uygulama güncellemesi (yeni kod deploy)

```bash
cd /opt/ecom

# Değişiklikleri çek (git kullanıyorsanız)
git pull origin main

# Sadece uygulamayı yeniden build edip başlat
docker compose up -d --build app
```

### Container kaynak kullanımı

```bash
docker stats
```

### Disk kullanımı

```bash
df -h /opt/ecom
du -sh /opt/ecom/app/*
```

---

## 14. Log Yönetimi

Uygulama logları `/opt/ecom/app/logs/` altında tutulur:

```
app/logs/
├── application.log      ← Genel uygulama logları
├── admin.log            ← Admin işlemleri (product/user/order yönetimi)
├── payment.log          ← Ödeme olayları
├── security.log         ← Login, logout, token yenileme olayları
└── error.log            ← Sadece ERROR seviyesindeki loglar
```

### Canlı log izleme

```bash
# Uygulama logları
tail -f /opt/ecom/app/logs/application.log

# Ödeme logları
tail -f /opt/ecom/app/logs/payment.log

# Güvenlik logları
tail -f /opt/ecom/app/logs/security.log

# Docker üzerinden
docker compose logs -f app
```

### Log dosyaları çok büyüdüğünde

Logback otomatik olarak günlük rotate eder ve 30 gün saklar (logback-spring.xml ayarı).
Manuel temizlemek için:

```bash
# 30 günden eski logları sil
find /opt/ecom/app/logs/ -name "*.log.*" -mtime +30 -delete
```

---

## 15. Yedekleme

### PostgreSQL yedekleme

```bash
# Yedek al
docker compose exec postgres pg_dump -U <POSTGRES_DB_USER> <POSTGRES_DB_NAME> \
  > /opt/ecom/backup_$(date +%Y%m%d_%H%M%S).sql

# Yedeği geri yükle
docker compose exec -T postgres psql -U <POSTGRES_DB_USER> <POSTGRES_DB_NAME> \
  < /opt/ecom/backup_YYYYMMDD_HHMMSS.sql
```

### Otomatik günlük yedekleme (cron)

```bash
(crontab -l 2>/dev/null; echo "0 2 * * * docker compose -f /opt/ecom/docker-compose.exec exec -T postgres pg_dump -U \$POSTGRES_DB_USER \$POSTGRES_DB_NAME > /opt/ecom/app/backups/db_\$(date +\%Y\%m\%d).sql 2>&1") | crontab -
```

### Yüklenen görseller

```bash
# Arşivle
tar -czf /opt/ecom/uploads_backup_$(date +%Y%m%d).tar.gz /opt/ecom/app/uploads/
```

### SSL sertifikalarını yedekle

```bash
tar -czf /opt/ecom/certbot_backup_$(date +%Y%m%d).tar.gz /opt/ecom/app/certbot/
```

---

## 16. Sorun Giderme

### Uygulama başlamıyor — log klasörü izin hatası

**Belirti:**
```
java.io.FileNotFoundException: /app/logs/admin.log (Permission denied)
```

**Çözüm:**
```bash
chmod 777 /opt/ecom/app/logs /opt/ecom/app/uploads
docker compose restart app
```

### Nginx crash ediyor — SSL sertifikası bulunamıyor

**Belirti:** `ecom_nginx` sürekli `Restarting` durumunda.

**Sebep:** Nginx başlarken `/etc/letsencrypt/live/<domain>/fullchain.pem` dosyasını arıyor. Sertifika alınmadan önce nginx başlatıldıysa crash eder.

**Çözüm:**
```bash
# Önce nginx'i durdur
docker compose stop nginx

# Standalone ile sertifika al (bkz. Bölüm 8)
docker run --rm -p 80:80 \
  -v /opt/ecom/app/certbot/conf:/etc/letsencrypt \
  certbot/certbot certonly --standalone \
  --email <email> --agree-tos --no-eff-email \
  -d <domain>

# Nginx'i başlat
docker compose start nginx
```

### docker compose run certbot takılı kalıyor

**Sebep:** Certbot service'in `docker-compose.yaml`'da özel bir `entrypoint` tanımlı. `docker compose run certbot <komut>` bu entrypoint'e argüman olarak geçer ve çalışmaz.

**Çözüm:** Doğrudan `docker run` kullan:

```bash
docker run --rm \
  -p 80:80 \
  -v /opt/ecom/app/certbot/conf:/etc/letsencrypt \
  certbot/certbot certonly --standalone ...
```

### .env değişkenlerine dikkat: Windows CRLF

Windows'tan SCP ile transfer edilen `.env` dosyalarında satır sonu `\r\n` olabilir.
Docker Compose `HOST_PORT=5353\r` olarak okuyunca "invalid hostPort" hatası verir.

**Çözüm:**
```bash
sed -i 's/\r//' /opt/ecom/.env
```

### Certbot renewal "Input the webroot" hatası

**Sebep:** Renewal config dosyasında `authenticator = standalone` var ama standalone modda nginx'in durdurulması gerekir.

**Çözüm:** Renewal config'i webroot'a çevir (bkz. Bölüm 8, Adım 3).

### Veritabanına bağlanamıyor

```bash
# PostgreSQL loglarını kontrol et
docker compose logs postgres

# Container içinden bağlantı testi
docker compose exec postgres psql -U <POSTGRES_DB_USER> -d <POSTGRES_DB_NAME> -c "SELECT 1;"
```

### Redis bağlantı hatası

```bash
# Redis loglarını kontrol et
docker compose logs redis

# Şifre ile ping testi
docker compose exec redis redis-cli -a <REDIS_PASSWORD> ping
# Beklenen: PONG
```

### Uygulama 502 Bad Gateway dönüyor

Nginx çalışıyor ama uygulama henüz hazır değil.

```bash
# Uygulama loglarına bak
docker compose logs app

# Health endpoint'i direkt uygulamadan kontrol et (nginx bypass)
curl http://localhost:<HOST_PORT>/actuator/health
```

---

## 17. Yeni Müşteri Deployment (Multi-Tenant)

Her müşteri için ayrı bir klasör, ayrı `.env` ve ayrı Docker stack kullanılır.
`TENANT_NAME` ve `HOST_PORT` değerleri farklı olmalıdır.

### Klasör yapısı

```
/opt/
├── ecom-musteri1/     ← Müşteri 1 (port 5353, domain: musteri1.selahattin.dev)
├── ecom-musteri2/     ← Müşteri 2 (port 5354, domain: musteri2.selahattin.dev)
└── ecom-musteri3/     ← Müşteri 3 (port 5355, domain: musteri3.selahattin.dev)
```

### Hızlı kurulum scripti

```bash
#!/bin/bash
# Kullanım: bash deploy-tenant.sh musteri2 musteri2.selahattin.dev 5354

TENANT=$1
DOMAIN=$2
PORT=$3

# Projeyi kopyala
cp -r /opt/ecom-template /opt/ecom-$TENANT

# .env oluştur
cp /opt/ecom-$TENANT/.env.example /opt/ecom-$TENANT/.env

# Temel değerleri ayarla
sed -i "s/TENANT_NAME=.*/TENANT_NAME=$TENANT/" /opt/ecom-$TENANT/.env
sed -i "s/DOMAIN_NAME=.*/DOMAIN_NAME=$DOMAIN/" /opt/ecom-$TENANT/.env
sed -i "s/HOST_PORT=.*/HOST_PORT=$PORT/" /opt/ecom-$TENANT/.env

echo "Lütfen /opt/ecom-$TENANT/.env dosyasını düzenleyerek şifre ve key değerlerini girin."
echo "Sonra: cd /opt/ecom-$TENANT && bash nginx/init-ssl.sh"
```

### Nginx — Çoklu subdomain yönetimi

Tek sunucuda birden fazla müşteri için `nginx/templates/default.conf.template` dosyasına
her müşteri için ayrı server block eklenir:

```nginx
server {
    listen 443 ssl;
    server_name musteri2.selahattin.dev;
    # ... SSL ve proxy_pass http://app2:5353
}
```

---

## Hızlı Referans — Sık Kullanılan Komutlar

```bash
# Stack durumu
docker compose ps

# Uygulama logları (canlı)
docker compose logs -f app

# Uygulamayı yeniden deploy et
docker compose up -d --build app

# SSL sertifika süresi
docker compose exec certbot certbot certificates

# SSL renewal test
docker compose exec certbot certbot renew --dry-run

# DB yedeği al
docker compose exec postgres pg_dump -U $POSTGRES_DB_USER $POSTGRES_DB_NAME > backup.sql

# Nginx reload (sertifika yenilendikten sonra)
docker compose exec nginx nginx -s reload

# Tüm container'ları yeniden başlat
docker compose restart

# Disk kullanımı
du -sh /opt/ecom/app/*

# Container kaynak kullanımı
docker stats
```

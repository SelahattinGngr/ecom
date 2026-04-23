#!/bin/bash
set -e

ENV_FILE="$(dirname "$0")/../.env"
if [ -f "$ENV_FILE" ]; then
    export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

if [ -z "$DOMAIN_NAME" ] || [ -z "$CERT_EMAIL" ]; then
    echo "DOMAIN_NAME ve CERT_EMAIL .env dosyasında tanımlı olmalı."
    exit 1
fi

echo "Domain: $DOMAIN_NAME"
echo "Email: $CERT_EMAIL"

# Nginx'i sadece HTTP modunda başlat (certbot challenge için)
docker compose up -d nginx

echo "Certbot ile sertifika alınıyor..."
docker compose run --rm certbot certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "$CERT_EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN_NAME"

echo "Nginx yeniden başlatılıyor..."
docker compose restart nginx

echo "SSL sertifikası başarıyla alındı."

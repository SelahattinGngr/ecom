#!/bin/sh
set -e

# Volume mount'tan gelen dizinlerin sahipliğini spring'e ver
chown -R spring:spring /app/assets /app/logs

# spring kullanıcısına geçip uygulamayı başlat
exec su-exec spring sh -c "java $JAVA_OPTS -jar /app/app.jar"
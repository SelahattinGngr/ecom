# ====== BUILD STAGE ======
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# ====== RUNTIME STAGE ======
FROM eclipse-temurin:21-jdk-alpine AS runtime

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

RUN apk add --no-cache su-exec

RUN mkdir -p assets/public/products logs

COPY --from=build /app/target/*.jar app.jar

COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN sed -i 's/\r//' /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV TZ=Europe/Istanbul

EXPOSE 5353

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:5353/actuator/health || exit 1

ENTRYPOINT ["/docker-entrypoint.sh"]
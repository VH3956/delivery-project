# ========================
# Stage 1: Build
# ========================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .

COPY api-gateway ./api-gateway
COPY eureka-server ./eureka-server
COPY user-service ./user-service
COPY order-service ./order-service
COPY delivery-service ./delivery-service

RUN mvn clean package -pl delivery-service -am -DskipTests --no-transfer-progress

# ========================
# Stage 2: Runtime
# ========================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/delivery-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
# ========================
# Stage 1: Build
# ========================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy Maven files for better layer caching
COPY pom.xml .

# Copy source code
COPY api-gateway ./api-gateway
COPY eureka-server ./eureka-server
COPY user-service ./user-service
COPY order-service ./order-service
COPY delivery-service ./delivery-service

# Build only this service
RUN mvn clean package -pl eureka-server -am -DskipTests --no-transfer-progress

# ========================
# Stage 2: Runtime
# ========================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/eureka-server/target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .

COPY api-gateway ./api-gateway
COPY eureka-server ./eureka-server
COPY user-service ./user-service
COPY order-service ./order-service
COPY delivery-service ./delivery-service

RUN mvn clean package -pl eureka-server -am -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/eureka-server/target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
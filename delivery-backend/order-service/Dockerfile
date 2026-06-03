# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .

COPY api-gateway ./api-gateway
COPY eureka-server ./eureka-server
COPY user-service ./user-service
COPY order-service ./order-service
COPY delivery-service ./delivery-service

RUN mvn clean package -pl order-service -am -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/order-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
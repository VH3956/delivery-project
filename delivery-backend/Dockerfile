# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder

ARG SERVICE_NAME

WORKDIR /app

COPY pom.xml .

COPY api-gateway ./api-gateway
COPY eureka-server ./eureka-server
COPY user-service ./user-service

RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine

ARG SERVICE_NAME

WORKDIR /app

COPY --from=builder /app/${SERVICE_NAME}/target/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
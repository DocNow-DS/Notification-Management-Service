# Multi-stage build for Java 17 Spring Boot application
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]

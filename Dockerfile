# Многостадийная сборка для ARM64
FROM --platform=linux/arm64 eclipse-temurin:17-jdk as builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

FROM --platform=linux/arm64 eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
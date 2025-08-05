# Используем многостадийную сборку
ARG BUILDPLATFORM=linux/arm64
ARG TARGETPLATFORM=linux/arm64

FROM --platform=$BUILDPLATFORM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

FROM --platform=$TARGETPLATFORM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]